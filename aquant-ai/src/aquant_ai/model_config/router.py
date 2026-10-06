from typing import Annotated

from fastapi import APIRouter, Depends, Query, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from aquant_ai.core.config import Settings, get_settings
from aquant_ai.core.database import get_db_session
from aquant_ai.core.security import CurrentUser, get_current_user
from aquant_ai.model_config.schema import (
    EntityIdRequest,
    ModelCreate,
    ModelResponse,
    ModelTestResponse,
    ModelUpdate,
    ModelUpdateRequest,
    ProviderCreate,
    ProviderResponse,
    ProviderUpdate,
    ProviderUpdateRequest,
)
from aquant_ai.model_config.service import ModelConfigService

router = APIRouter(prefix="/modelConfig", tags=["modelConfig"])

SessionDep = Annotated[AsyncSession, Depends(get_db_session)]
SettingsDep = Annotated[Settings, Depends(get_settings)]
CurrentUserDep = Annotated[CurrentUser, Depends(get_current_user)]
ProviderIdQuery = Annotated[int, Query(alias="providerId", gt=0)]


def get_service(
    session: SessionDep,
    settings: SettingsDep,
) -> ModelConfigService:
    """按请求创建服务实例，共用当前请求的数据库会话。"""
    return ModelConfigService(session, settings)


ServiceDep = Annotated[ModelConfigService, Depends(get_service)]


@router.get("/provider/list", response_model=list[ProviderResponse])
async def list_providers(
    user: CurrentUserDep,
    service: ServiceDep,
) -> list[ProviderResponse]:
    return await service.list_providers(user.user_id)


@router.post(
    "/provider/create",
    response_model=ProviderResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_provider(
    payload: ProviderCreate,
    user: CurrentUserDep,
    service: ServiceDep,
) -> ProviderResponse:
    return await service.create_provider(user.user_id, payload)


@router.post("/provider/update", response_model=ProviderResponse)
async def update_provider(
    payload: ProviderUpdateRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> ProviderResponse:
    update_payload = ProviderUpdate.model_validate(payload.model_dump(exclude={"id"}))
    return await service.update_provider(user.user_id, payload.id, update_payload)


@router.post("/provider/delete", status_code=status.HTTP_204_NO_CONTENT)
async def delete_provider(
    payload: EntityIdRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> Response:
    await service.delete_provider(user.user_id, payload.id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/model/list", response_model=list[ModelResponse])
async def list_models(
    user: CurrentUserDep,
    service: ServiceDep,
) -> list[ModelResponse]:
    return await service.list_models(user.user_id)


@router.get("/model/available", response_model=list[str])
async def list_available_models(
    provider_id: ProviderIdQuery,
    user: CurrentUserDep,
    service: ServiceDep,
) -> list[str]:
    """从供应商接口获取当前凭据可用的模型标识。"""
    return await service.list_available_models(user.user_id, provider_id)


@router.post(
    "/model/create",
    response_model=ModelResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_model(
    payload: ModelCreate,
    user: CurrentUserDep,
    service: ServiceDep,
) -> ModelResponse:
    return await service.create_model(user.user_id, payload)


@router.post("/model/update", response_model=ModelResponse)
async def update_model(
    payload: ModelUpdateRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> ModelResponse:
    update_payload = ModelUpdate.model_validate(payload.model_dump(exclude={"id"}))
    return await service.update_model(user.user_id, payload.id, update_payload)


@router.post("/model/delete", status_code=status.HTTP_204_NO_CONTENT)
async def delete_model(
    payload: EntityIdRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> Response:
    await service.delete_model(user.user_id, payload.id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/model/setDefault", response_model=ModelResponse)
async def set_default_model(
    payload: EntityIdRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> ModelResponse:
    return await service.set_default(user.user_id, payload.id)


@router.post("/model/test", response_model=ModelTestResponse)
async def test_model(
    payload: EntityIdRequest,
    user: CurrentUserDep,
    service: ServiceDep,
) -> ModelTestResponse:
    return await service.test_model(user.user_id, payload.id)
