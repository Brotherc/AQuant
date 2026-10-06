import asyncio
import ipaddress
import socket
import time
from datetime import datetime
from urllib.parse import urlsplit

from fastapi import HTTPException, status
from openai import AsyncOpenAI
from sqlalchemy import delete, func, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from aquant_ai.core.config import Settings
from aquant_ai.core.security import CredentialCipher
from aquant_ai.model_config.entity import ModelConfig, ModelProviderConfig
from aquant_ai.model_config.schema import (
    ApiMode,
    ModelCreate,
    ModelResponse,
    ModelTestResponse,
    ModelUpdate,
    ProviderCreate,
    ProviderResponse,
    ProviderType,
    ProviderUpdate,
)

DEFAULT_BASE_URLS = {
    ProviderType.OPENAI: "https://api.openai.com/v1",
    ProviderType.DEEPSEEK: "https://api.deepseek.com",
    ProviderType.QWEN: "https://dashscope.aliyuncs.com/compatible-mode/v1",
    ProviderType.GEMINI: "https://generativelanguage.googleapis.com/v1beta/openai/",
    ProviderType.MOONSHOT: "https://api.moonshot.cn/v1",
    ProviderType.ZHIPU: "https://open.bigmodel.cn/api/paas/v4",
}

# 除 OpenAI 外的预置供应商优先使用兼容范围更稳定的 Chat Completions。
DEFAULT_API_MODES = {
    ProviderType.OPENAI: ApiMode.RESPONSES,
    ProviderType.DEEPSEEK: ApiMode.CHAT_COMPLETIONS,
    ProviderType.QWEN: ApiMode.CHAT_COMPLETIONS,
    ProviderType.GEMINI: ApiMode.CHAT_COMPLETIONS,
    ProviderType.MOONSHOT: ApiMode.CHAT_COMPLETIONS,
    ProviderType.ZHIPU: ApiMode.CHAT_COMPLETIONS,
    ProviderType.OPENAI_COMPATIBLE: ApiMode.CHAT_COMPLETIONS,
}

# 这些模型不是持仓会诊使用的文本生成模型，不应自动声明 Agent 能力。
NON_TEXT_MODEL_MARKERS = (
    "embedding",
    "moderation",
    "whisper",
    "transcribe",
    "tts",
    "dall-e",
    "image",
    "realtime",
    "audio",
)

REASONING_MODEL_MARKERS = (
    "reasoner",
    "reasoning",
    "deepseek-r1",
    "qwq",
    "thinking",
    "glm-z1",
)


class ModelConfigService:
    """管理用户的供应商、模型能力、默认模型和连接测试。"""

    def __init__(self, session: AsyncSession, settings: Settings):
        self.session = session
        self.settings = settings

    async def list_providers(self, user_id: int) -> list[ProviderResponse]:
        """查询当前用户的供应商，并一次性统计各供应商的模型数量。"""
        statement = (
            select(ModelProviderConfig, func.count(ModelConfig.id))
            .outerjoin(ModelConfig, ModelConfig.provider_id == ModelProviderConfig.id)
            .where(ModelProviderConfig.user_id == user_id)
            .group_by(ModelProviderConfig.id)
            .order_by(ModelProviderConfig.created_at.asc())
        )
        rows = (await self.session.execute(statement)).all()
        return [self._provider_response(provider, model_count) for provider, model_count in rows]

    async def create_provider(self, user_id: int, payload: ProviderCreate) -> ProviderResponse:
        """创建供应商连接，并在入库前加密 API Key。"""
        base_url = payload.base_url or DEFAULT_BASE_URLS.get(payload.provider_type)
        if payload.provider_type == ProviderType.OPENAI_COMPATIBLE and not base_url:
            raise HTTPException(status_code=422, detail="自定义兼容服务必须填写 Base URL")
        provider = ModelProviderConfig(
            user_id=user_id,
            name=payload.name,
            provider_type=payload.provider_type.value,
            base_url=base_url,
            enabled=True,
        )
        self._set_api_key(provider, payload.api_key)
        self.session.add(provider)
        await self._commit_unique("供应商配置名称已存在")
        await self.session.refresh(provider)
        return self._provider_response(provider, 0)

    async def update_provider(
        self, user_id: int, provider_id: int, payload: ProviderUpdate
    ) -> ProviderResponse:
        """更新供应商；未提交新密钥时保留原密钥。"""
        provider = await self._get_provider(user_id, provider_id)
        values = payload.model_dump(exclude_unset=True, exclude={"api_key", "clear_api_key"})
        if "provider_type" in values:
            values["provider_type"] = values["provider_type"].value
        for field, value in values.items():
            setattr(provider, field, value)
        # 启用状态不再由用户维护，编辑旧配置时同步恢复为可用状态。
        provider.enabled = True
        provider_type = ProviderType(provider.provider_type)
        if not provider.base_url:
            provider.base_url = DEFAULT_BASE_URLS.get(provider_type)
        if payload.clear_api_key:
            provider.api_key_encrypted = None
            provider.api_key_hint = None
        elif payload.api_key is not None:
            self._set_api_key(provider, payload.api_key)
        if provider.provider_type == ProviderType.OPENAI_COMPATIBLE.value and not provider.base_url:
            raise HTTPException(status_code=422, detail="自定义兼容服务必须填写 Base URL")
        await self._commit_unique("供应商配置名称已存在")
        model_count = await self.session.scalar(
            select(func.count(ModelConfig.id)).where(ModelConfig.provider_id == provider.id)
        )
        return self._provider_response(provider, model_count or 0)

    async def delete_provider(self, user_id: int, provider_id: int) -> None:
        """删除供应商及其关联模型。"""
        provider = await self._get_provider(user_id, provider_id)
        await self.session.execute(
            delete(ModelConfig).where(ModelConfig.provider_id == provider.id)
        )
        await self.session.delete(provider)
        await self.session.commit()

    async def list_models(self, user_id: int) -> list[ModelResponse]:
        """查询当前用户的模型，并把默认模型排在首位。"""
        statement = (
            select(ModelConfig, ModelProviderConfig)
            .join(ModelProviderConfig, ModelProviderConfig.id == ModelConfig.provider_id)
            .where(ModelConfig.user_id == user_id)
            .order_by(ModelConfig.is_default.desc(), ModelConfig.created_at.asc())
        )
        rows = (await self.session.execute(statement)).all()
        return [self._model_response(model, provider) for model, provider in rows]

    async def list_available_models(self, user_id: int, provider_id: int) -> list[str]:
        """使用已保存的供应商凭据获取真实模型列表，避免用户手工填写模型标识。"""
        provider = await self._get_provider(user_id, provider_id)
        await self._validate_resolved_address(provider.base_url, provider.provider_type)
        api_key = self._decrypt_api_key(provider)
        client = AsyncOpenAI(
            api_key=api_key or "not-required",
            base_url=provider.base_url,
            timeout=20,
        )
        try:
            page = await client.models.list()
            return sorted(
                {
                    model.id.strip()
                    for model in page.data
                    if isinstance(model.id, str) and model.id.strip()
                },
                key=str.casefold,
            )
        except Exception as exc:
            message = self._safe_error_message(exc)
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail=f"获取供应商模型列表失败：{message}",
            ) from exc
        finally:
            await client.close()

    async def create_model(self, user_id: int, payload: ModelCreate) -> ModelResponse:
        """在属于当前用户的供应商下创建模型配置。"""
        provider = await self._get_provider(user_id, payload.provider_id)
        provider_type = ProviderType(provider.provider_type)
        api_mode = (
            payload.api_mode
            if "api_mode" in payload.model_fields_set
            else DEFAULT_API_MODES[provider_type]
        )
        capabilities = self._infer_model_capabilities(provider_type, payload.model_name)
        extra_settings = {
            **payload.extra_settings,
            "capabilities_source": "INFERRED",
        }
        default_model_id = await self.session.scalar(
            select(ModelConfig.id)
            .where(ModelConfig.user_id == user_id, ModelConfig.is_default.is_(True))
            .limit(1)
        )
        should_set_default = payload.is_default or default_model_id is None
        model = ModelConfig(
            user_id=user_id,
            provider_id=provider.id,
            **payload.model_dump(
                exclude={"provider_id", "api_mode", "is_default", "extra_settings"}
            ),
            api_mode=api_mode.value,
            **capabilities,
            extra_settings=extra_settings,
            is_default=should_set_default,
            enabled=True,
        )
        if should_set_default:
            await self._clear_default(user_id)
            provider.enabled = True
        self.session.add(model)
        await self._commit_unique("该供应商下的模型标识已存在")
        await self.session.refresh(model)
        return self._model_response(model, provider)

    async def update_model(
        self, user_id: int, model_id: int, payload: ModelUpdate
    ) -> ModelResponse:
        """更新模型；设为默认时同步清除该用户原来的默认模型。"""
        model, provider = await self._get_model(user_id, model_id)
        values = payload.model_dump(exclude_unset=True)
        if "provider_id" in values:
            provider = await self._get_provider(user_id, values["provider_id"])
            if "api_mode" not in values:
                values["api_mode"] = DEFAULT_API_MODES[ProviderType(provider.provider_type)].value
        if "api_mode" in values:
            api_mode = values["api_mode"]
            values["api_mode"] = api_mode.value if isinstance(api_mode, ApiMode) else api_mode
        if "provider_id" in values or "model_name" in values:
            provider_type = ProviderType(provider.provider_type)
            model_name = values.get("model_name", model.model_name)
            values.update(self._infer_model_capabilities(provider_type, model_name))
            extra_settings = dict(values.get("extra_settings") or model.extra_settings or {})
            extra_settings["capabilities_source"] = "INFERRED"
            values["extra_settings"] = extra_settings
        should_set_default = values.pop("is_default", None) is True
        if should_set_default:
            await self._clear_default(user_id, exclude_id=model.id)
        for field, value in values.items():
            setattr(model, field, value)
        # 启用状态不再对用户开放，保存配置时统一消除旧数据中的停用状态。
        model.enabled = True
        provider.enabled = True
        if should_set_default:
            model.is_default = True
        await self._commit_unique("该供应商下的模型标识已存在")
        await self.session.refresh(model)
        return self._model_response(model, provider)

    async def delete_model(self, user_id: int, model_id: int) -> None:
        """删除当前用户拥有的模型配置。"""
        model, _ = await self._get_model(user_id, model_id)
        await self.session.delete(model)
        await self.session.commit()

    async def set_default(self, user_id: int, model_id: int) -> ModelResponse:
        """设置唯一默认模型，并自动恢复模型及所属供应商的可用状态。"""
        model, provider = await self._get_model(user_id, model_id)
        await self._clear_default(user_id, exclude_id=model.id)
        model.is_default = True
        model.enabled = True
        provider.enabled = True
        await self.session.commit()
        await self.session.refresh(model)
        return self._model_response(model, provider)

    async def test_model(self, user_id: int, model_id: int) -> ModelTestResponse:
        """按模型选择的 API 模式发送最小文本请求，验证基础连通性。"""
        model, provider = await self._get_model(user_id, model_id)
        model.enabled = True
        provider.enabled = True
        capabilities = self._infer_model_capabilities(
            ProviderType(provider.provider_type), model.model_name
        )
        for field, value in capabilities.items():
            setattr(model, field, value)
        model.extra_settings = {
            **(model.extra_settings or {}),
            "capabilities_source": "INFERRED",
        }
        await self._validate_resolved_address(provider.base_url, provider.provider_type)
        api_key = self._decrypt_api_key(provider)
        started = time.perf_counter()
        try:
            client = AsyncOpenAI(
                api_key=api_key or "not-required",
                base_url=provider.base_url,
                timeout=model.request_timeout_seconds,
            )
            if model.api_mode == ApiMode.RESPONSES.value:
                await client.responses.create(
                    model=model.model_name,
                    input="只回复 OK",
                    max_output_tokens=16,
                )
            else:
                await client.chat.completions.create(
                    model=model.model_name,
                    messages=[{"role": "user", "content": "只回复 OK"}],
                    max_tokens=16,
                )
            latency_ms = round((time.perf_counter() - started) * 1000)
            model.last_test_status = "SUCCESS"
            model.last_test_message = "连接成功"
            result = ModelTestResponse(success=True, message="连接成功", latency_ms=latency_ms)
        except Exception as exc:
            latency_ms = round((time.perf_counter() - started) * 1000)
            error_message = self._safe_error_message(exc)
            model.last_test_status = "FAILED"
            model.last_test_message = error_message
            result = ModelTestResponse(success=False, message=error_message, latency_ms=latency_ms)
        model.last_test_at = datetime.now()
        await self.session.commit()
        return result

    async def get_runtime_model(
        self, user_id: int, model_id: int | None = None
    ) -> tuple[ModelConfig, ModelProviderConfig]:
        """获取 Agent 执行使用的模型，并再次校验目标地址。"""
        statement = (
            select(ModelConfig, ModelProviderConfig)
            .join(ModelProviderConfig, ModelProviderConfig.id == ModelConfig.provider_id)
            .where(ModelConfig.user_id == user_id)
        )
        if model_id is None:
            statement = statement.where(ModelConfig.is_default.is_(True))
        else:
            statement = statement.where(ModelConfig.id == model_id)
        row = (await self.session.execute(statement)).one_or_none()
        if row is None:
            raise HTTPException(status_code=422, detail="未找到默认模型配置")
        model, provider = row
        await self._validate_resolved_address(provider.base_url, provider.provider_type)
        return model, provider

    def decrypt_provider_api_key(self, provider: ModelProviderConfig) -> str | None:
        """仅供运行时客户端构造使用，不应通过接口返回结果。"""
        return self._decrypt_api_key(provider)

    async def _get_provider(self, user_id: int, provider_id: int) -> ModelProviderConfig:
        provider = await self.session.scalar(
            select(ModelProviderConfig).where(
                ModelProviderConfig.id == provider_id,
                ModelProviderConfig.user_id == user_id,
            )
        )
        if provider is None:
            raise HTTPException(status_code=404, detail="模型供应商配置不存在")
        return provider

    async def _get_model(
        self, user_id: int, model_id: int
    ) -> tuple[ModelConfig, ModelProviderConfig]:
        row = (
            await self.session.execute(
                select(ModelConfig, ModelProviderConfig)
                .join(ModelProviderConfig, ModelProviderConfig.id == ModelConfig.provider_id)
                .where(ModelConfig.id == model_id, ModelConfig.user_id == user_id)
            )
        ).one_or_none()
        if row is None:
            raise HTTPException(status_code=404, detail="模型配置不存在")
        model, provider = row
        return model, provider

    async def _clear_default(self, user_id: int, exclude_id: int | None = None) -> None:
        """清除用户已有默认标记，exclude_id 用于保留即将设置的模型。"""
        statement = update(ModelConfig).where(ModelConfig.user_id == user_id)
        if exclude_id is not None:
            statement = statement.where(ModelConfig.id != exclude_id)
        await self.session.execute(statement.values(is_default=False))

    def _set_api_key(self, provider: ModelProviderConfig, api_key: str | None) -> None:
        if api_key is None:
            return
        normalized = api_key.strip()
        try:
            cipher = CredentialCipher(self.settings.model_credential_master_key)
        except RuntimeError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc
        provider.api_key_encrypted = cipher.encrypt(normalized)
        provider.api_key_hint = f"****{normalized[-4:]}" if len(normalized) >= 4 else "****"

    def _decrypt_api_key(self, provider: ModelProviderConfig) -> str | None:
        if not provider.api_key_encrypted:
            return None
        try:
            return CredentialCipher(self.settings.model_credential_master_key).decrypt(
                provider.api_key_encrypted
            )
        except RuntimeError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc

    @staticmethod
    def _infer_model_capabilities(
        provider_type: ProviderType,
        model_name: str,
    ) -> dict[str, bool]:
        """根据供应商和模型系列保守推断能力，未知能力一律不默认开启。"""
        normalized_name = model_name.strip().lower()
        is_text_model = not any(marker in normalized_name for marker in NON_TEXT_MODEL_MARKERS)
        is_known_provider = provider_type != ProviderType.OPENAI_COMPATIBLE
        supports_reasoning = is_text_model and (
            any(marker in normalized_name for marker in REASONING_MODEL_MARKERS)
            or normalized_name.startswith(("o1", "o3", "o4", "gpt-5", "qwen3"))
            or "gemini-2.5" in normalized_name
        )
        return {
            "supports_tool_calling": is_text_model and is_known_provider,
            "supports_structured_output": is_text_model
            and provider_type in {ProviderType.OPENAI, ProviderType.GEMINI},
            "supports_streaming": is_text_model and is_known_provider,
            "supports_reasoning": supports_reasoning,
        }

    async def _commit_unique(self, message: str) -> None:
        """提交事务，并将数据库唯一键冲突转换为稳定的业务错误。"""
        try:
            await self.session.commit()
        except IntegrityError as exc:
            await self.session.rollback()
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=message) from exc

    @staticmethod
    async def _validate_resolved_address(
        base_url: str | None,
        provider_type: str | None = None,
    ) -> None:
        """官方固定地址兼容代理 Fake-IP，其他地址仍执行 DNS 层 SSRF 校验。"""
        if not base_url:
            return
        try:
            provider = ProviderType(provider_type) if provider_type else None
        except ValueError:
            provider = None
        official_base_url = DEFAULT_BASE_URLS.get(provider) if provider else None
        if official_base_url and base_url.rstrip("/") == official_base_url.rstrip("/"):
            return
        parsed = urlsplit(base_url)
        loop = asyncio.get_running_loop()
        try:
            infos = await loop.getaddrinfo(
                parsed.hostname,
                parsed.port or 443,
                type=socket.SOCK_STREAM,
            )
        except socket.gaierror as exc:
            raise HTTPException(status_code=422, detail="模型服务域名无法解析") from exc
        if any(not ipaddress.ip_address(info[4][0]).is_global for info in infos):
            raise HTTPException(status_code=422, detail="模型服务地址解析到了非公网地址")

    @staticmethod
    def _safe_error_message(exc: Exception) -> str:
        message = str(exc).replace("\n", " ").strip()
        return (message or exc.__class__.__name__)[:500]

    @staticmethod
    def _provider_response(provider: ModelProviderConfig, model_count: int) -> ProviderResponse:
        return ProviderResponse(
            id=provider.id,
            name=provider.name,
            provider_type=ProviderType(provider.provider_type),
            base_url=provider.base_url,
            has_api_key=bool(provider.api_key_encrypted),
            api_key_hint=provider.api_key_hint,
            enabled=provider.enabled,
            model_count=model_count,
            created_at=provider.created_at,
            updated_at=provider.updated_at,
        )

    @staticmethod
    def _model_response(model: ModelConfig, provider: ModelProviderConfig) -> ModelResponse:
        return ModelResponse(
            id=model.id,
            provider_id=model.provider_id,
            provider_name=provider.name,
            provider_type=ProviderType(provider.provider_type),
            name=model.name,
            model_name=model.model_name,
            api_mode=ApiMode(model.api_mode),
            supports_tool_calling=model.supports_tool_calling,
            supports_structured_output=model.supports_structured_output,
            supports_streaming=model.supports_streaming,
            supports_reasoning=model.supports_reasoning,
            request_timeout_seconds=model.request_timeout_seconds,
            extra_settings=model.extra_settings or {},
            is_default=model.is_default,
            enabled=model.enabled,
            last_test_status=model.last_test_status,
            last_test_message=model.last_test_message,
            last_test_at=model.last_test_at,
            created_at=model.created_at,
            updated_at=model.updated_at,
        )
