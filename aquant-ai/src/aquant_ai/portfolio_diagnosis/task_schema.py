from datetime import date, datetime
from enum import StrEnum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator


class DiagnosisStatus(StrEnum):
    """会诊任务状态。"""

    PENDING = "PENDING"
    COLLECTING = "COLLECTING"
    ANALYZING = "ANALYZING"
    SYNTHESIZING = "SYNTHESIZING"
    SUCCESS = "SUCCESS"
    PARTIAL_SUCCESS = "PARTIAL_SUCCESS"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class RoleResultStatus(StrEnum):
    """专业角色执行状态。"""

    PENDING = "PENDING"
    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class DiagnosisStartRequest(BaseModel):
    """创建持仓会诊任务的请求。"""

    portfolio_id: int
    model_id: int | None = None
    request_id: str | None = Field(default=None, max_length=64)

    @field_validator("request_id")
    @classmethod
    def normalize_request_id(cls, value: str | None) -> str | None:
        normalized = value.strip() if value else None
        return normalized or None


class DiagnosisActionRequest(BaseModel):
    """重试或取消会诊任务的请求。"""

    diagnosis_id: int


class DiagnosisRoleResultResponse(BaseModel):
    """专业角色执行结果响应。"""

    model_config = ConfigDict(from_attributes=True)

    id: int
    diagnosis_id: int
    role_code: str
    role_name: str
    model_id: int
    status: RoleResultStatus
    prompt_version: str
    result_json: dict[str, Any] | None
    input_tokens: int | None
    output_tokens: int | None
    latency_ms: int | None
    error_message: str | None
    started_at: datetime | None
    completed_at: datetime | None
    created_at: datetime
    updated_at: datetime


class DiagnosisListItemResponse(BaseModel):
    """会诊历史列表项，不返回体积较大的快照和完整报告。"""

    model_config = ConfigDict(from_attributes=True)

    id: int
    portfolio_id: int
    model_id: int
    request_id: str
    status: DiagnosisStatus
    workflow_version: str
    data_as_of: date | None
    risk_level: str | None
    summary: str | None
    error_message: str | None
    started_at: datetime | None
    completed_at: datetime | None
    created_at: datetime
    updated_at: datetime


class DiagnosisDetailResponse(DiagnosisListItemResponse):
    """包含输入快照、最终报告和角色结果的会诊详情。"""

    prompt_version: str | None
    context_snapshot: dict[str, Any] | None
    context_hash: str | None
    report_json: dict[str, Any] | None
    role_results: list[DiagnosisRoleResultResponse] = Field(default_factory=list)
