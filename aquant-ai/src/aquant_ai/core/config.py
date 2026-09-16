from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
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
    openai_api_key: str | None = Field(default=None, repr=False)
    openai_model: str | None = None


@lru_cache
def get_settings() -> Settings:
    return Settings()

