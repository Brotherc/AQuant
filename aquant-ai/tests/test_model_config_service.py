import asyncio
from types import SimpleNamespace

import pytest
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from aquant_ai.core.config import Settings
from aquant_ai.core.database import Base
from aquant_ai.model_config.schema import (
    ModelCreate,
    ProviderCreate,
    ProviderType,
    ProviderUpdate,
)
from aquant_ai.model_config.service import ModelConfigService


def test_create_provider_and_default_model() -> None:
    async def run() -> None:
        engine = create_async_engine("sqlite+aiosqlite:///:memory:")
        async with engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)

        session_factory = async_sessionmaker(engine, expire_on_commit=False)
        settings = Settings(model_credential_master_key="AQuant@2025!ModelCredentialKey#1")

        async with session_factory() as session:
            service = ModelConfigService(session, settings)
            provider = await service.create_provider(
                7,
                ProviderCreate(
                    name="个人 OpenAI",
                    provider_type=ProviderType.OPENAI,
                    api_key="sk-secret-value",
                ),
            )
            model = await service.create_model(
                7,
                ModelCreate(
                    provider_id=provider.id,
                    name="持仓分析",
                    model_name="gpt-test",
                    is_default=True,
                ),
            )

            assert provider.base_url == "https://api.openai.com/v1"
            assert provider.has_api_key is True
            assert provider.api_key_hint == "****alue"
            assert model.is_default is True
            assert len(await service.list_models(7)) == 1

            updated = await service.update_provider(
                7,
                provider.id,
                ProviderUpdate(base_url=None),
            )
            assert updated.base_url == "https://api.openai.com/v1"

            await service.delete_provider(7, provider.id)
            assert await service.list_providers(7) == []
            assert await service.list_models(7) == []

        await engine.dispose()

    asyncio.run(run())


def test_deleting_default_model_does_not_select_a_replacement() -> None:
    """首个模型自动成为默认模型，但删除后不自动指定其他默认模型。"""

    async def run() -> None:
        engine = create_async_engine("sqlite+aiosqlite:///:memory:")
        async with engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)

        session_factory = async_sessionmaker(engine, expire_on_commit=False)
        settings = Settings(model_credential_master_key="AQuant@2025!ModelCredentialKey#1")
        async with session_factory() as session:
            service = ModelConfigService(session, settings)
            provider = await service.create_provider(
                7,
                ProviderCreate(
                    name="个人 OpenAI",
                    provider_type=ProviderType.OPENAI,
                    api_key="sk-secret-value",
                ),
            )
            first_model = await service.create_model(
                7,
                ModelCreate(
                    provider_id=provider.id,
                    name="主模型",
                    model_name="gpt-primary",
                ),
            )
            second_model = await service.create_model(
                7,
                ModelCreate(
                    provider_id=provider.id,
                    name="备用模型",
                    model_name="gpt-fallback",
                ),
            )

            assert first_model.is_default is True
            assert second_model.is_default is False

            await service.delete_model(7, first_model.id)
            remaining_models = await service.list_models(7)
            assert len(remaining_models) == 1
            assert remaining_models[0].id == second_model.id
            assert remaining_models[0].is_default is False

        await engine.dispose()

    asyncio.run(run())


def test_openai_compatible_provider_defaults() -> None:
    async def run() -> None:
        engine = create_async_engine("sqlite+aiosqlite:///:memory:")
        async with engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)

        session_factory = async_sessionmaker(engine, expire_on_commit=False)
        settings = Settings(model_credential_master_key="AQuant@2025!ModelCredentialKey#1")
        expected_urls = {
            ProviderType.GEMINI: "https://generativelanguage.googleapis.com/v1beta/openai/",
            ProviderType.MOONSHOT: "https://api.moonshot.cn/v1",
            ProviderType.ZHIPU: "https://open.bigmodel.cn/api/paas/v4",
        }

        async with session_factory() as session:
            service = ModelConfigService(session, settings)
            for provider_type, expected_url in expected_urls.items():
                provider = await service.create_provider(
                    7,
                    ProviderCreate(
                        name=provider_type.value,
                        provider_type=provider_type,
                        api_key=f"{provider_type.value.lower()}-secret",
                    ),
                )
                model = await service.create_model(
                    7,
                    ModelCreate(
                        provider_id=provider.id,
                        name=f"{provider_type.value} 模型",
                        model_name=f"{provider_type.value.lower()}-model",
                    ),
                )

                assert provider.base_url == expected_url
                assert model.api_mode == "CHAT_COMPLETIONS"

        await engine.dispose()

    asyncio.run(run())


def test_list_available_models_uses_provider_credentials(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """模型列表应由供应商实时返回，并在响应前去重、排序。"""

    class FakeModels:
        async def list(self) -> SimpleNamespace:
            return SimpleNamespace(
                data=[
                    SimpleNamespace(id="gpt-z"),
                    SimpleNamespace(id="gpt-a"),
                    SimpleNamespace(id="gpt-z"),
                ]
            )

    class FakeOpenAI:
        def __init__(self, **kwargs: object):
            assert kwargs["api_key"] == "sk-secret-value"
            self.models = FakeModels()

        async def close(self) -> None:
            return None

    async def skip_address_validation(
        _base_url: str | None,
        _provider_type: str | None = None,
    ) -> None:
        return None

    async def run() -> None:
        engine = create_async_engine("sqlite+aiosqlite:///:memory:")
        async with engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)

        session_factory = async_sessionmaker(engine, expire_on_commit=False)
        settings = Settings(model_credential_master_key="AQuant@2025!ModelCredentialKey#1")
        async with session_factory() as session:
            service = ModelConfigService(session, settings)
            provider = await service.create_provider(
                7,
                ProviderCreate(
                    name="个人 OpenAI",
                    provider_type=ProviderType.OPENAI,
                    api_key="sk-secret-value",
                ),
            )
            monkeypatch.setattr(
                "aquant_ai.model_config.service.AsyncOpenAI",
                FakeOpenAI,
            )
            monkeypatch.setattr(
                service,
                "_validate_resolved_address",
                skip_address_validation,
            )

            assert await service.list_available_models(7, provider.id) == [
                "gpt-a",
                "gpt-z",
            ]

        await engine.dispose()

    asyncio.run(run())


def test_official_provider_address_skips_fake_ip_dns_validation(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """固定官方地址不应因本地代理返回 Fake-IP 而阻断模型请求。"""

    def fail_if_dns_is_resolved() -> None:
        raise AssertionError("官方固定地址不应执行 DNS 公网校验")

    async def run() -> None:
        monkeypatch.setattr(
            "aquant_ai.model_config.service.asyncio.get_running_loop",
            fail_if_dns_is_resolved,
        )
        await ModelConfigService._validate_resolved_address(
            "https://api.openai.com/v1/",
            ProviderType.OPENAI.value,
        )
        with pytest.raises(AssertionError, match="不应执行 DNS"):
            await ModelConfigService._validate_resolved_address(
                "https://api.openai.com/v1",
                ProviderType.OPENAI_COMPATIBLE.value,
            )

    asyncio.run(run())


@pytest.mark.parametrize(
    ("provider_type", "model_name", "expected"),
    [
        (
            ProviderType.OPENAI,
            "gpt-5",
            {
                "supports_tool_calling": True,
                "supports_structured_output": True,
                "supports_streaming": True,
                "supports_reasoning": True,
            },
        ),
        (
            ProviderType.OPENAI,
            "text-embedding-3-small",
            {
                "supports_tool_calling": False,
                "supports_structured_output": False,
                "supports_streaming": False,
                "supports_reasoning": False,
            },
        ),
        (
            ProviderType.DEEPSEEK,
            "deepseek-chat",
            {
                "supports_tool_calling": True,
                "supports_structured_output": False,
                "supports_streaming": True,
                "supports_reasoning": False,
            },
        ),
        (
            ProviderType.OPENAI_COMPATIBLE,
            "custom-chat-model",
            {
                "supports_tool_calling": False,
                "supports_structured_output": False,
                "supports_streaming": False,
                "supports_reasoning": False,
            },
        ),
    ],
)
def test_infer_model_capabilities(
    provider_type: ProviderType,
    model_name: str,
    expected: dict[str, bool],
) -> None:
    assert ModelConfigService._infer_model_capabilities(provider_type, model_name) == expected
