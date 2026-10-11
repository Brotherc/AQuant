from typing import Annotated

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from aquant_ai.core.database import get_db_session
from aquant_ai.core.security import CurrentUser, get_current_user
from aquant_ai.portfolio_diagnosis.service import PortfolioDiagnosisService
from aquant_ai.portfolio_diagnosis.task_schema import (
    DiagnosisActionRequest,
    DiagnosisDetailResponse,
    DiagnosisListItemResponse,
    DiagnosisStartRequest,
)

router = APIRouter(prefix="/portfolioDiagnosis", tags=["portfolioDiagnosis"])

SessionDep = Annotated[AsyncSession, Depends(get_db_session)]
CurrentUserDep = Annotated[CurrentUser, Depends(get_current_user)]
DiagnosisIdQuery = Annotated[int, Query(alias="diagnosisId")]
PortfolioIdQuery = Annotated[int, Query(alias="portfolioId")]


def get_service(session: SessionDep) -> PortfolioDiagnosisService:
    """按请求创建会诊服务实例。"""
    return PortfolioDiagnosisService(session)


ServiceDep = Annotated[PortfolioDiagnosisService, Depends(get_service)]


@router.post(
    "/start",
    response_model=DiagnosisDetailResponse,
    status_code=status.HTTP_201_CREATED,
)
async def start_diagnosis(
    payload: DiagnosisStartRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> DiagnosisDetailResponse:
    return await service.start(user.user_id, payload)


@router.get("/detail", response_model=DiagnosisDetailResponse)
async def get_diagnosis_detail(
    diagnosis_id: DiagnosisIdQuery,
    user: CurrentUserDep,
    service: ServiceDep,
) -> DiagnosisDetailResponse:
    return await service.detail(user.user_id, diagnosis_id)


@router.get("/list", response_model=list[DiagnosisListItemResponse])
async def list_diagnoses(
    portfolio_id: PortfolioIdQuery,
    user: CurrentUserDep,
    service: ServiceDep,
) -> list[DiagnosisListItemResponse]:
    return await service.list(user.user_id, portfolio_id)


@router.post("/retry", response_model=DiagnosisDetailResponse)
async def retry_diagnosis(
    payload: DiagnosisActionRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> DiagnosisDetailResponse:
    return await service.retry(user.user_id, payload.diagnosis_id)


@router.post("/cancel", response_model=DiagnosisDetailResponse)
async def cancel_diagnosis(
    payload: DiagnosisActionRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> DiagnosisDetailResponse:
    return await service.cancel(user.user_id, payload.diagnosis_id)
