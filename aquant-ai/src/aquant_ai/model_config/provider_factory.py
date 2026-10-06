from dataclasses import dataclass

from agents import RunConfig
from agents.models.openai_provider import OpenAIProvider
from sqlalchemy.ext.asyncio import AsyncSession

from aquant_ai.core.config import Settings
from aquant_ai.model_config.schema import ApiMode
from aquant_ai.model_config.service import ModelConfigService


@dataclass(frozen=True)
class RuntimeModel:
    """Agent 执行一次任务所需的模型标识和运行配置。"""

    model_name: str
    run_config: RunConfig


class ModelProviderFactory:
    """把用户保存的模型配置转换为 OpenAI Agents SDK 运行配置。"""

    def __init__(self, session: AsyncSession, settings: Settings):
        self.service = ModelConfigService(session, settings)
        self.settings = settings

    async def create(self, user_id: int, model_id: int | None = None) -> RuntimeModel:
        """按指定模型或用户默认模型构造隔离的 Agent 运行环境。"""
        model, provider_config = await self.service.get_runtime_model(user_id, model_id)
        provider = OpenAIProvider(
            api_key=self.service.decrypt_provider_api_key(provider_config) or "not-required",
            base_url=provider_config.base_url,
            use_responses=model.api_mode == ApiMode.RESPONSES.value,
        )
        return RuntimeModel(
            model_name=model.model_name,
            run_config=RunConfig(
                model=model.model_name,
                model_provider=provider,
                tracing_disabled=not self.settings.agent_tracing_enabled,
                # 即使启用 tracing，也不上传提示词、工具参数等敏感业务数据。
                trace_include_sensitive_data=False,
                workflow_name="AQuant portfolio workflow",
            ),
        )
