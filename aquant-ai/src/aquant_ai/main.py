from fastapi import FastAPI

from aquant_ai import __version__
from aquant_ai.api.router import api_router
from aquant_ai.core.config import get_settings


def create_app() -> FastAPI:
    settings = get_settings()
    application = FastAPI(
        title=settings.app_name,
        version=__version__,
        docs_url="/docs",
        redoc_url="/redoc",
    )
    application.include_router(api_router, prefix=settings.api_prefix)
    return application


app = create_app()

