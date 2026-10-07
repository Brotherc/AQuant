from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field


def to_camel(value: str) -> str:
    """将 Python 字段名转换为 Java 接口使用的驼峰名称。"""
    head, *tail = value.split("_")
    return head + "".join(part.capitalize() for part in tail)


class BackendModel(BaseModel):
    """Java 接口模型基类，统一处理驼峰字段并拒绝意外字段。"""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
    )


class PortfolioInfo(BackendModel):
    id: int
    name: str
    base_currency: str
    benchmark_code: str | None = None


class PortfolioAccount(BackendModel):
    id: int
    account_name: str
    broker_code: str | None = None
    broker_name: str | None = None
    account_type: str | None = None
    sync_mode: str | None = None
    status: str | None = None


class CashBalance(BackendModel):
    account_id: int
    account_name: str | None = None
    currency: str
    total_balance: Decimal
    available_balance: Decimal | None = None
    frozen_balance: Decimal | None = None
    update_time: datetime | None = None


class Position(BackendModel):
    id: int
    account_id: int
    account_name: str | None = None
    asset_type: str
    market: str | None = None
    asset_code: str
    asset_name: str | None = None
    currency: str
    quantity: Decimal
    available_quantity: Decimal | None = None
    cost_price: Decimal | None = None
    cost_amount: Decimal | None = None
    latest_price: Decimal | None = None
    market_value: Decimal | None = None
    unrealized_profit: Decimal | None = None
    unrealized_profit_rate: Decimal | None = None
    day_income: Decimal | None = None
    day_income_rate: Decimal | None = None
    position_ratio: Decimal | None = None
    quote_date: date | None = None


class PortfolioOverview(BackendModel):
    portfolio_id: int
    portfolio_name: str
    base_currency: str
    cash_amount: Decimal
    market_value: Decimal
    total_asset: Decimal
    cost_amount: Decimal
    unrealized_profit: Decimal
    unrealized_profit_rate: Decimal | None = None
    position_count: int
    unpriced_asset_count: int
    unsupported_currency_count: int
    calculate_time: datetime
    positions: list[Position] = Field(default_factory=list)


class AssetSnapshot(BackendModel):
    account_id: int
    account_name: str | None = None
    snapshot_date: date
    currency: str
    cash_amount: Decimal
    market_value: Decimal
    total_asset: Decimal
    cost_amount: Decimal
    unrealized_profit: Decimal
    unpriced_asset_count: int


class RecentTrade(BackendModel):
    id: int
    account_id: int
    account_name: str | None = None
    asset_type: str
    market: str | None = None
    asset_code: str | None = None
    asset_name: str | None = None
    trade_type: str
    trade_time: datetime
    settlement_date: date | None = None
    quantity: Decimal | None = None
    price: Decimal | None = None
    gross_amount: Decimal | None = None
    total_fee: Decimal
    net_amount: Decimal | None = None
    currency: str
    source: str


class DataQuality(BackendModel):
    latest_quote_date: date | None = None
    missing_price_asset_codes: list[str] = Field(default_factory=list)
    stale_price_asset_codes: list[str] = Field(default_factory=list)
    unsupported_currencies: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)


class PortfolioDiagnosisContext(BackendModel):
    """Java 主服务返回的完整持仓会诊数据快照。"""

    schema_version: str
    generated_at: datetime
    data_as_of: date | None = None
    trade_range_start: date
    trade_limit: int
    portfolio: PortfolioInfo
    overview: PortfolioOverview
    accounts: list[PortfolioAccount] = Field(default_factory=list)
    cash_balances: list[CashBalance] = Field(default_factory=list)
    positions: list[Position] = Field(default_factory=list)
    asset_snapshots: list[AssetSnapshot] = Field(default_factory=list)
    recent_trades: list[RecentTrade] = Field(default_factory=list)
    data_quality: DataQuality


class BackendResponse(BackendModel):
    """Java 服务统一响应结构。"""

    success: bool
    code: int
    message: str | None = None
    data: PortfolioDiagnosisContext | None = None
