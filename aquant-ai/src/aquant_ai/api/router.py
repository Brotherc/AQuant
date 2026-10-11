from fastapi import APIRouter

from aquant_ai.health.router import router as health_router
from aquant_ai.model_config.router import router as model_config_router
from aquant_ai.portfolio_diagnosis.router import router as portfolio_diagnosis_router

api_router = APIRouter()
api_router.include_router(health_router)
api_router.include_router(model_config_router)
api_router.include_router(portfolio_diagnosis_router)
