import asyncio

import pytest
from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from aquant_ai.core.database import Base
from aquant_ai.model_config.entity import ModelConfig
from aquant_ai.portfolio_diagnosis.router import router as portfolio_diagnosis_router
from aquant_ai.portfolio_diagnosis.service import PortfolioDiagnosisService
from aquant_ai.portfolio_diagnosis.task_schema import (
    DiagnosisStartRequest,
    DiagnosisStatus,
)


async def create_session() -> tuple[AsyncSession, object]:
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    session_factory = async_sessionmaker(engine, expire_on_commit=False)
    return session_factory(), engine


async def add_model(
    session: AsyncSession,
    user_id: int,
    *,
    is_default: bool = True,
) -> ModelConfig:
    model = ModelConfig(
        user_id=user_id,
        provider_id=1,
        name=f"用户{user_id}模型",
        model_name=f"model-{user_id}",
        api_mode="RESPONSES",
        supports_tool_calling=True,
        supports_structured_output=True,
        supports_streaming=True,
        supports_reasoning=False,
        request_timeout_seconds=60,
        extra_settings={},
        is_default=is_default,
        enabled=True,
    )
    session.add(model)
    await session.commit()
    await session.refresh(model)
    return model


def test_start_is_idempotent_and_isolated_by_user() -> None:
    async def run() -> None:
        session, engine = await create_session()
        async with session:
            model = await add_model(session, 7)
            service = PortfolioDiagnosisService(session)
            request = DiagnosisStartRequest(portfolio_id=10, request_id="request-1")

            first = await service.start(7, request)
            second = await service.start(7, request)

            assert first.id == second.id
            assert first.model_id == model.id
            assert first.status == DiagnosisStatus.PENDING
            assert len(await service.list(7, 10)) == 1
            assert await service.list(8, 10) == []
            with pytest.raises(HTTPException) as exc_info:
                await service.detail(8, first.id)
            assert exc_info.value.status_code == 404
        await engine.dispose()

    asyncio.run(run())


def test_status_machine_rejects_skipped_steps_and_allows_success_path() -> None:
    async def run() -> None:
        session, engine = await create_session()
        async with session:
            await add_model(session, 7)
            service = PortfolioDiagnosisService(session)
            diagnosis = await service.start(7, DiagnosisStartRequest(portfolio_id=10))

            with pytest.raises(HTTPException) as exc_info:
                await service.transition_status(7, diagnosis.id, DiagnosisStatus.SUCCESS)
            assert exc_info.value.status_code == 409

            collecting = await service.transition_status(
                7, diagnosis.id, DiagnosisStatus.COLLECTING
            )
            analyzing = await service.transition_status(
                7, diagnosis.id, DiagnosisStatus.ANALYZING
            )
            synthesizing = await service.transition_status(
                7, diagnosis.id, DiagnosisStatus.SYNTHESIZING
            )
            succeeded = await service.transition_status(
                7, diagnosis.id, DiagnosisStatus.SUCCESS
            )

            assert collecting.started_at is not None
            assert analyzing.status == DiagnosisStatus.ANALYZING
            assert synthesizing.status == DiagnosisStatus.SYNTHESIZING
            assert succeeded.status == DiagnosisStatus.SUCCESS
            assert succeeded.completed_at is not None
        await engine.dispose()

    asyncio.run(run())


def test_failed_task_can_retry_and_pending_task_can_cancel() -> None:
    async def run() -> None:
        session, engine = await create_session()
        async with session:
            await add_model(session, 7)
            service = PortfolioDiagnosisService(session)
            failed_task = await service.start(7, DiagnosisStartRequest(portfolio_id=10))
            await service.transition_status(
                7, failed_task.id, DiagnosisStatus.COLLECTING
            )
            failed = await service.transition_status(
                7,
                failed_task.id,
                DiagnosisStatus.FAILED,
                error_message="模拟失败",
            )
            retried = await service.retry(7, failed.id)

            assert failed.error_message == "模拟失败"
            assert retried.status == DiagnosisStatus.PENDING
            assert retried.error_message is None
            assert retried.started_at is None
            assert retried.completed_at is None

            cancellable = await service.start(7, DiagnosisStartRequest(portfolio_id=11))
            cancelled = await service.cancel(7, cancellable.id)
            assert cancelled.status == DiagnosisStatus.CANCELLED
            with pytest.raises(HTTPException) as exc_info:
                await service.cancel(7, cancellable.id)
            assert exc_info.value.status_code == 409
        await engine.dispose()

    asyncio.run(run())


def test_explicit_model_must_belong_to_current_user() -> None:
    async def run() -> None:
        session, engine = await create_session()
        async with session:
            other_users_model = await add_model(session, 8)
            service = PortfolioDiagnosisService(session)

            with pytest.raises(HTTPException) as exc_info:
                await service.start(
                    7,
                    DiagnosisStartRequest(
                        portfolio_id=10,
                        model_id=other_users_model.id,
                    ),
                )
            assert exc_info.value.status_code == 422
        await engine.dispose()

    asyncio.run(run())


def test_portfolio_diagnosis_routes_follow_api_conventions() -> None:
    """会诊接口仅使用 GET/POST，且不使用路径参数。"""
    assert portfolio_diagnosis_router.routes
    for route in portfolio_diagnosis_router.routes:
        assert "{" not in route.path
        assert route.methods <= {"GET", "POST"}
