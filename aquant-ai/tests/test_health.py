from fastapi.testclient import TestClient

from aquant_ai.main import app


def test_health() -> None:
    response = TestClient(app).get("/api/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "service": "AQuant AI",
        "environment": "development",
        "version": "0.1.0",
    }

