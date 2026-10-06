from datetime import datetime
from typing import Any

from sqlalchemy import (
    JSON,
    BigInteger,
    Boolean,
    DateTime,
    Integer,
    String,
    Text,
)
from sqlalchemy.orm import Mapped, mapped_column

from aquant_ai.core.database import Base


class ModelProviderConfig(Base):
    """用户配置的模型供应商连接，同一连接可以复用给多个模型。"""

    __tablename__ = "ai_model_provider"

    # 供应商配置主键。
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    # 配置所属的用户 ID。
    user_id: Mapped[int] = mapped_column(BigInteger)
    # 用户自定义的供应商配置名称。
    name: Mapped[str] = mapped_column(String)
    # 供应商类型，例如 OpenAI、DeepSeek 或 OpenAI 兼容服务。
    provider_type: Mapped[str] = mapped_column(String)
    # 模型服务的 API 基础地址。
    base_url: Mapped[str | None] = mapped_column(String)
    # AES-256-GCM 加密后的 API Key，任何查询接口都不得返回密文或明文。
    api_key_encrypted: Mapped[str | None] = mapped_column(Text)
    # API Key 的末四位脱敏提示。
    api_key_hint: Mapped[str | None] = mapped_column(String)
    # 供应商配置是否启用。
    enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    # 供应商配置的创建时间。
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    # 供应商配置的最后更新时间。
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.now, onupdate=datetime.now
    )


class ModelConfig(Base):
    """具体模型及其 Agent 能力配置。"""

    __tablename__ = "ai_model_config"

    # 模型配置主键。
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    # 配置所属的用户 ID。
    user_id: Mapped[int] = mapped_column(BigInteger)
    # 关联的模型供应商配置 ID。
    provider_id: Mapped[int] = mapped_column(Integer)
    # 用户自定义的模型显示名称。
    name: Mapped[str] = mapped_column(String)
    # 供应商定义的模型标识，例如 gpt-5 或 deepseek-chat。
    model_name: Mapped[str] = mapped_column(String)
    # 模型调用协议，例如 Responses API 或 Chat Completions API。
    api_mode: Mapped[str] = mapped_column(String, default="RESPONSES")
    # 系统识别的 Agent 工具调用能力。
    supports_tool_calling: Mapped[bool] = mapped_column(Boolean, default=False)
    # 系统识别的 Schema 结构化输出能力。
    supports_structured_output: Mapped[bool] = mapped_column(Boolean, default=False)
    # 系统识别的流式响应能力。
    supports_streaming: Mapped[bool] = mapped_column(Boolean, default=False)
    # 系统识别的推理模型能力。
    supports_reasoning: Mapped[bool] = mapped_column(Boolean, default=False)
    # 单次模型请求的超时时间，单位为秒。
    request_timeout_seconds: Mapped[int] = mapped_column(Integer, default=60)
    # 供应商或模型特有的扩展调用参数。
    extra_settings: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict)
    # 是否为当前用户的默认模型。
    is_default: Mapped[bool] = mapped_column(Boolean, default=False)
    # 模型配置是否启用。
    enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    # 最近一次连接测试状态，例如 SUCCESS 或 FAILED。
    last_test_status: Mapped[str | None] = mapped_column(String)
    # 最近一次连接测试返回的提示或错误信息。
    last_test_message: Mapped[str | None] = mapped_column(String)
    # 最近一次执行连接测试的时间。
    last_test_at: Mapped[datetime | None] = mapped_column(DateTime)
    # 模型配置的创建时间。
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    # 模型配置的最后更新时间。
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.now, onupdate=datetime.now
    )
