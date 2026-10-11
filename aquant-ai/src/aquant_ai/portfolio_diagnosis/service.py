from datetime import datetime
from uuid import uuid4

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from aquant_ai.model_config.entity import ModelConfig
from aquant_ai.portfolio_diagnosis.entity import PortfolioDiagnosis
from aquant_ai.portfolio_diagnosis.repository import PortfolioDiagnosisRepository
from aquant_ai.portfolio_diagnosis.task_schema import (
    DiagnosisDetailResponse,
    DiagnosisListItemResponse,
    DiagnosisRoleResultResponse,
    DiagnosisStartRequest,
    DiagnosisStatus,
)

WORKFLOW_VERSION = "portfolio-diagnosis-v1"
TERMINAL_STATUSES = {
    DiagnosisStatus.SUCCESS,
    DiagnosisStatus.PARTIAL_SUCCESS,
    DiagnosisStatus.FAILED,
    DiagnosisStatus.CANCELLED,
}
ALLOWED_TRANSITIONS = {
    DiagnosisStatus.PENDING: {DiagnosisStatus.COLLECTING, DiagnosisStatus.CANCELLED},
    DiagnosisStatus.COLLECTING: {
        DiagnosisStatus.ANALYZING,
        DiagnosisStatus.FAILED,
        DiagnosisStatus.CANCELLED,
    },
    DiagnosisStatus.ANALYZING: {
        DiagnosisStatus.SYNTHESIZING,
        DiagnosisStatus.FAILED,
        DiagnosisStatus.CANCELLED,
    },
    DiagnosisStatus.SYNTHESIZING: {
        DiagnosisStatus.SUCCESS,
        DiagnosisStatus.PARTIAL_SUCCESS,
        DiagnosisStatus.FAILED,
        DiagnosisStatus.CANCELLED,
    },
}


class PortfolioDiagnosisService:
    """管理会诊任务的持久化、用户隔离、幂等和状态流转。"""

    def __init__(self, session: AsyncSession):
        self.session = session
        self.repository = PortfolioDiagnosisRepository(session)

    async def start(
        self, user_id: int, payload: DiagnosisStartRequest
    ) -> DiagnosisDetailResponse:
        """创建待执行任务，阶段二不调用模型或启动 Workflow。"""
        request_id = payload.request_id or uuid4().hex
        existing = await self.repository.get_by_request_id(user_id, request_id)
        if existing is not None:
            if existing.portfolio_id != payload.portfolio_id or (
                payload.model_id is not None and existing.model_id != payload.model_id
            ):
                raise HTTPException(status_code=409, detail="请求幂等标识已被其他会诊任务使用")
            return await self._detail_response(existing)

        model_id = await self._resolve_model_id(user_id, payload.model_id)
        diagnosis = PortfolioDiagnosis(
            user_id=user_id,
            portfolio_id=payload.portfolio_id,
            model_id=model_id,
            request_id=request_id,
            status=str(DiagnosisStatus.PENDING),
            workflow_version=WORKFLOW_VERSION,
        )
        self.session.add(diagnosis)
        try:
            await self.session.commit()
        except IntegrityError as exc:
            await self.session.rollback()
            concurrent = await self.repository.get_by_request_id(user_id, request_id)
            if concurrent is None:
                raise HTTPException(status_code=409, detail="创建会诊任务失败") from exc
            return await self._detail_response(concurrent)
        await self.session.refresh(diagnosis)
        return await self._detail_response(diagnosis)

    async def detail(self, user_id: int, diagnosis_id: int) -> DiagnosisDetailResponse:
        """查询当前用户拥有的会诊任务及角色结果。"""
        diagnosis = await self._get_owned(user_id, diagnosis_id)
        return await self._detail_response(diagnosis)

    async def list(
        self, user_id: int, portfolio_id: int
    ) -> list[DiagnosisListItemResponse]:
        """查询当前用户指定组合的会诊历史。"""
        diagnoses = await self.repository.list_owned(user_id, portfolio_id)
        return [DiagnosisListItemResponse.model_validate(item) for item in diagnoses]

    async def retry(self, user_id: int, diagnosis_id: int) -> DiagnosisDetailResponse:
        """把失败或部分成功任务恢复为待执行，并清理旧角色结果。"""
        diagnosis = await self._get_owned(user_id, diagnosis_id)
        current = DiagnosisStatus(diagnosis.status)
        if current not in {DiagnosisStatus.FAILED, DiagnosisStatus.PARTIAL_SUCCESS}:
            raise HTTPException(status_code=409, detail="只有失败或部分成功的会诊任务可以重试")
        await self.repository.delete_role_results(diagnosis.id)
        diagnosis.status = str(DiagnosisStatus.PENDING)
        diagnosis.risk_level = None
        diagnosis.report_json = None
        diagnosis.summary = None
        diagnosis.error_message = None
        diagnosis.started_at = None
        diagnosis.completed_at = None
        await self.session.commit()
        await self.session.refresh(diagnosis)
        return await self._detail_response(diagnosis)

    async def cancel(self, user_id: int, diagnosis_id: int) -> DiagnosisDetailResponse:
        """取消尚未结束的任务，终态任务不允许重复取消。"""
        diagnosis = await self._get_owned(user_id, diagnosis_id)
        return await self.transition_status(
            user_id,
            diagnosis.id,
            DiagnosisStatus.CANCELLED,
        )

    async def transition_status(
        self,
        user_id: int,
        diagnosis_id: int,
        target_status: DiagnosisStatus,
        error_message: str | None = None,
    ) -> DiagnosisDetailResponse:
        """按固定状态机推进任务，供后续 Workflow 编排层调用。"""
        diagnosis = await self._get_owned(user_id, diagnosis_id)
        current_status = DiagnosisStatus(diagnosis.status)
        if target_status not in ALLOWED_TRANSITIONS.get(current_status, set()):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail=f"会诊任务不能从 {current_status.value} 变更为 {target_status.value}",
            )
        now = datetime.now()
        if diagnosis.started_at is None and target_status != DiagnosisStatus.CANCELLED:
            diagnosis.started_at = now
        diagnosis.status = str(target_status)
        diagnosis.error_message = error_message if target_status == DiagnosisStatus.FAILED else None
        diagnosis.completed_at = now if target_status in TERMINAL_STATUSES else None
        await self.session.commit()
        await self.session.refresh(diagnosis)
        return await self._detail_response(diagnosis)

    async def _resolve_model_id(self, user_id: int, model_id: int | None) -> int:
        statement = select(ModelConfig.id).where(ModelConfig.user_id == user_id)
        if model_id is None:
            statement = statement.where(ModelConfig.is_default.is_(True))
        else:
            statement = statement.where(ModelConfig.id == model_id)
        resolved = await self.session.scalar(statement.limit(1))
        if resolved is None:
            raise HTTPException(status_code=422, detail="未找到可用于会诊的模型配置")
        return resolved

    async def _get_owned(self, user_id: int, diagnosis_id: int) -> PortfolioDiagnosis:
        diagnosis = await self.repository.get_owned(user_id, diagnosis_id)
        if diagnosis is None:
            raise HTTPException(status_code=404, detail="持仓会诊任务不存在")
        return diagnosis

    async def _detail_response(
        self, diagnosis: PortfolioDiagnosis
    ) -> DiagnosisDetailResponse:
        roles = await self.repository.list_role_results(diagnosis.id)
        payload = DiagnosisDetailResponse.model_validate(diagnosis).model_copy(
            update={
                "role_results": [
                    DiagnosisRoleResultResponse.model_validate(role) for role in roles
                ]
            }
        )
        return payload
