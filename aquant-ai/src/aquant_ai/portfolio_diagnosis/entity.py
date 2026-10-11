from datetime import date, datetime
from typing import Any

from sqlalchemy import JSON, BigInteger, Date, DateTime, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from aquant_ai.core.database import Base


class PortfolioDiagnosis(Base):
    """一次角色化持仓会诊任务及其最终报告。"""

    __tablename__ = "ai_portfolio_diagnosis"

    # 会诊任务主键。
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    # 任务所属用户 ID，用于所有查询的数据隔离。
    user_id: Mapped[int] = mapped_column(BigInteger)
    # 本次会诊对应的投资组合 ID。
    portfolio_id: Mapped[int] = mapped_column(BigInteger)
    # 创建任务时固化的模型配置 ID。
    model_id: Mapped[int] = mapped_column(Integer)
    # 用户范围内的请求幂等标识。
    request_id: Mapped[str] = mapped_column(String)
    # 会诊任务当前状态。
    status: Mapped[str] = mapped_column(String, default="PENDING")
    # 本次任务使用的 Workflow 版本。
    workflow_version: Mapped[str] = mapped_column(String)
    # 主审角色使用的 Prompt 版本，尚未执行时为空。
    prompt_version: Mapped[str | None] = mapped_column(String)
    # 从 Java 服务固化的不可变会诊输入快照。
    context_snapshot: Mapped[dict[str, Any] | None] = mapped_column(JSON)
    # 输入快照的 SHA-256 摘要。
    context_hash: Mapped[str | None] = mapped_column(String)
    # 快照包含的最新业务数据日期。
    data_as_of: Mapped[date | None] = mapped_column(Date)
    # 最终组合风险等级。
    risk_level: Mapped[str | None] = mapped_column(String)
    # Workflow 生成的结构化最终报告。
    report_json: Mapped[dict[str, Any] | None] = mapped_column(JSON)
    # 历史任务列表使用的报告摘要。
    summary: Mapped[str | None] = mapped_column(Text)
    # 脱敏后的任务失败原因。
    error_message: Mapped[str | None] = mapped_column(String)
    # 任务首次开始执行的时间。
    started_at: Mapped[datetime | None] = mapped_column(DateTime)
    # 任务进入终态的时间。
    completed_at: Mapped[datetime | None] = mapped_column(DateTime)
    # 任务创建时间。
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    # 任务最后更新时间。
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.now, onupdate=datetime.now
    )


class PortfolioDiagnosisRoleResult(Base):
    """一次会诊中单个专业角色的独立执行结果。"""

    __tablename__ = "ai_portfolio_diagnosis_role_result"

    # 角色执行结果主键。
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    # 所属会诊任务 ID。
    diagnosis_id: Mapped[int] = mapped_column(Integer)
    # 稳定的角色代码。
    role_code: Mapped[str] = mapped_column(String)
    # 用于界面展示的角色名称。
    role_name: Mapped[str] = mapped_column(String)
    # 角色实际使用的模型配置 ID。
    model_id: Mapped[int] = mapped_column(Integer)
    # 角色执行状态。
    status: Mapped[str] = mapped_column(String)
    # 角色 Prompt 版本。
    prompt_version: Mapped[str] = mapped_column(String)
    # 模型返回并通过校验的结构化角色结论。
    result_json: Mapped[dict[str, Any] | None] = mapped_column(JSON)
    # 模型调用消耗的输入 Token 数量。
    input_tokens: Mapped[int | None] = mapped_column(Integer)
    # 模型调用消耗的输出 Token 数量。
    output_tokens: Mapped[int | None] = mapped_column(Integer)
    # 角色执行耗时，单位为毫秒。
    latency_ms: Mapped[int | None] = mapped_column(Integer)
    # 脱敏后的角色失败原因。
    error_message: Mapped[str | None] = mapped_column(String)
    # 角色开始执行的时间。
    started_at: Mapped[datetime | None] = mapped_column(DateTime)
    # 角色执行完成的时间。
    completed_at: Mapped[datetime | None] = mapped_column(DateTime)
    # 角色结果创建时间。
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    # 角色结果最后更新时间。
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.now, onupdate=datetime.now
    )
