from fastapi.testclient import TestClient

from healthvault_ai.main import app

client = TestClient(app)


def test_root():
    r = client.get("/")
    assert r.status_code == 200
    assert r.json()["service"] == "ai-api"


def test_health():
    r = client.get("/health")
    assert r.json() == {"status": "ok"}
