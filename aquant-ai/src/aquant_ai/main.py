from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from aquant_ai import __version__
from aquant_ai.api.router import api_router
from aquant_ai.core.config import get_settings
from aquant_ai.core.database import engine


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    """应用关闭时释放数据库连接池，不自动创建或修改数据库表。"""
    yield
    await engine.dispose()


def create_app() -> FastAPI:
    """创建并装配 AQuant AI 的 FastAPI 应用。"""
    settings = get_settings()
    application = FastAPI(
        title=settings.app_name,
        version=__version__,
        docs_url="/docs",
        redoc_url="/redoc",
        lifespan=lifespan,
    )
    application.include_router(api_router, prefix=settings.api_prefix)
    return application


app = create_app()
