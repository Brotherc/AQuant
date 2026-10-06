from fastapi.testclient import TestClient

from aquant_ai.main import app
from aquant_ai.model_config.router import router as model_config_router


def test_health() -> None:
    with TestClient(app) as client:
        response = client.get("/api/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "service": "AQuant AI",
        "environment": "development",
        "version": "0.1.0",
    }


def test_model_config_routes_only_use_get_or_post_without_path_parameters() -> None:
    routes = model_config_router.routes

    assert routes
    for route in routes:
        assert "{" not in route.path
        assert route.methods <= {"GET", "POST"}
