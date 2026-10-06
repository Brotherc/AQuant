from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """AQuant AI 运行配置，默认从项目根目录的 .env 文件读取。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "AQuant AI"
    app_env: str = "development"
    app_host: str = "0.0.0.0"
    app_port: int = 8085
    api_prefix: str = "/api"
    # 本地默认连接 Java 示例服务使用的 MySQL 数据库，可通过 DATABASE_URL 覆盖。
    database_url: str = (
        "mysql+asyncmy://test:testtest@127.0.0.1:3306/nocobase?charset=utf8mb4"
    )
    # 本地默认与 Java 示例服务一致，可通过 AQUANT_JWT_SECRET 覆盖。
    aquant_jwt_secret: str = Field(default="AQuant@2025!SecretKey#Jwt", repr=False)
    # 用于加密和解密模型 API Key，按 UTF-8 编码后必须正好为 32 字节。
    model_credential_master_key: str | None = Field(default="AQuant@2026!ModelCredentialKey#1", repr=False)
    # 是否启用 Agents SDK tracing。开启后会记录 Agent 执行、模型及工具调用、交接和
    # 护栏检查等链路，用于排查流程问题与分析耗时；需另行配置可用的 tracing exporter，
    # 当前运行配置仍会关闭敏感输入输出采集。
    agent_tracing_enabled: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()
