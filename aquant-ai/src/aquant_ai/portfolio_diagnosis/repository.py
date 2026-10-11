from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from aquant_ai.portfolio_diagnosis.entity import (
    PortfolioDiagnosis,
    PortfolioDiagnosisRoleResult,
)


class PortfolioDiagnosisRepository:
    """持仓会诊任务及角色结果的数据访问层。"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def get_owned(self, user_id: int, diagnosis_id: int) -> PortfolioDiagnosis | None:
        """按任务 ID 和用户 ID 查询，避免跨用户读取。"""
        return await self.session.scalar(
            select(PortfolioDiagnosis).where(
                PortfolioDiagnosis.id == diagnosis_id,
                PortfolioDiagnosis.user_id == user_id,
            )
        )

    async def get_by_request_id(
        self, user_id: int, request_id: str
    ) -> PortfolioDiagnosis | None:
        """查询当前用户已使用的幂等请求标识。"""
        return await self.session.scalar(
            select(PortfolioDiagnosis).where(
                PortfolioDiagnosis.user_id == user_id,
                PortfolioDiagnosis.request_id == request_id,
            )
        )

    async def list_owned(self, user_id: int, portfolio_id: int) -> list[PortfolioDiagnosis]:
        """按创建时间倒序查询用户指定组合的会诊历史。"""
        rows = await self.session.scalars(
            select(PortfolioDiagnosis)
            .where(
                PortfolioDiagnosis.user_id == user_id,
                PortfolioDiagnosis.portfolio_id == portfolio_id,
            )
            .order_by(PortfolioDiagnosis.created_at.desc(), PortfolioDiagnosis.id.desc())
        )
        return list(rows)

    async def list_role_results(
        self, diagnosis_id: int
    ) -> list[PortfolioDiagnosisRoleResult]:
        """查询任务下的全部角色结果。"""
        rows = await self.session.scalars(
            select(PortfolioDiagnosisRoleResult)
            .where(PortfolioDiagnosisRoleResult.diagnosis_id == diagnosis_id)
            .order_by(PortfolioDiagnosisRoleResult.id.asc())
        )
        return list(rows)

    async def delete_role_results(self, diagnosis_id: int) -> None:
        """重试或未来删除任务时，由代码显式清理关联角色结果。"""
        await self.session.execute(
            delete(PortfolioDiagnosisRoleResult).where(
                PortfolioDiagnosisRoleResult.diagnosis_id == diagnosis_id
            )
        )
