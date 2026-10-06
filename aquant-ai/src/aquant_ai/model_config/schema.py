"""模型配置业务使用的请求、响应及枚举定义。"""

from datetime import datetime
from enum import StrEnum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator

from aquant_ai.core.security import validate_public_https_url


class ProviderType(StrEnum):
    """系统支持的供应商类型。"""

    OPENAI = "OPENAI"
    DEEPSEEK = "DEEPSEEK"
    QWEN = "QWEN"
    GEMINI = "GEMINI"
    MOONSHOT = "MOONSHOT"
    ZHIPU = "ZHIPU"
    OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE"


class ApiMode(StrEnum):
    """模型调用使用的 OpenAI API 协议。"""

    RESPONSES = "RESPONSES"
    CHAT_COMPLETIONS = "CHAT_COMPLETIONS"


class ProviderCreate(BaseModel):
    """创建模型供应商连接的请求。"""

    name: str = Field(min_length=1, max_length=100)
    provider_type: ProviderType
    base_url: str | None = Field(default=None, max_length=500)
    api_key: str | None = Field(default=None, min_length=1, max_length=2000)

    @field_validator("name")
    @classmethod
    def normalize_name(cls, value: str) -> str:
        return value.strip()

    @field_validator("base_url")
    @classmethod
    def validate_base_url(cls, value: str | None) -> str | None:
        return validate_public_https_url(value) if value else None


class ProviderUpdate(BaseModel):
    """更新供应商连接的请求，未传字段保持不变。"""

    name: str | None = Field(default=None, min_length=1, max_length=100)
    provider_type: ProviderType | None = None
    base_url: str | None = Field(default=None, max_length=500)
    api_key: str | None = Field(default=None, min_length=1, max_length=2000)
    clear_api_key: bool = False

    @field_validator("name")
    @classmethod
    def normalize_name(cls, value: str | None) -> str | None:
        return value.strip() if value else value

    @field_validator("base_url")
    @classmethod
    def validate_base_url(cls, value: str | None) -> str | None:
        return validate_public_https_url(value) if value else None


class ProviderUpdateRequest(ProviderUpdate):
    """携带供应商 ID 的更新请求。"""

    id: int = Field(gt=0)


class ProviderResponse(BaseModel):
    """供应商连接响应；只包含密钥状态和掩码，不返回密文或明文。"""

    id: int
    name: str
    provider_type: ProviderType
    base_url: str | None
    has_api_key: bool
    api_key_hint: str | None
    enabled: bool
    model_count: int = 0
    created_at: datetime
    updated_at: datetime


class ModelCreate(BaseModel):
    """创建模型配置的请求。"""

    provider_id: int
    name: str = Field(min_length=1, max_length=100)
    model_name: str = Field(min_length=1, max_length=200)
    api_mode: ApiMode = ApiMode.RESPONSES
    request_timeout_seconds: int = Field(default=60, ge=5, le=300)
    extra_settings: dict[str, Any] = Field(default_factory=dict)
    is_default: bool = False

    @field_validator("name", "model_name")
    @classmethod
    def strip_text(cls, value: str) -> str:
        return value.strip()


class ModelUpdate(BaseModel):
    """更新模型配置的请求。"""

    provider_id: int | None = None
    name: str | None = Field(default=None, min_length=1, max_length=100)
    model_name: str | None = Field(default=None, min_length=1, max_length=200)
    api_mode: ApiMode | None = None
    request_timeout_seconds: int | None = Field(default=None, ge=5, le=300)
    extra_settings: dict[str, Any] | None = None
    is_default: bool | None = None

    @field_validator("name", "model_name")
    @classmethod
    def strip_text(cls, value: str | None) -> str | None:
        return value.strip() if value else value


class ModelUpdateRequest(ModelUpdate):
    """携带模型 ID 的更新请求。"""

    id: int = Field(gt=0)


class EntityIdRequest(BaseModel):
    """删除、测试和设置默认模型等操作使用的 ID 请求。"""

    id: int = Field(gt=0)


class ModelResponse(BaseModel):
    """供模型配置页面展示的完整模型信息。"""

    model_config = ConfigDict(from_attributes=True)

    id: int
    provider_id: int
    provider_name: str
    provider_type: ProviderType
    name: str
    model_name: str
    api_mode: ApiMode
    supports_tool_calling: bool
    supports_structured_output: bool
    supports_streaming: bool
    supports_reasoning: bool
    request_timeout_seconds: int
    extra_settings: dict[str, Any]
    is_default: bool
    enabled: bool
    last_test_status: str | None
    last_test_message: str | None
    last_test_at: datetime | None
    created_at: datetime
    updated_at: datetime


class ModelTestResponse(BaseModel):
    """模型连接测试结果。"""

    success: bool
    message: str
    latency_ms: int
