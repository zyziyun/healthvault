from fastapi.testclient import TestClient

from healthvault_ai.main import app

client = TestClient(app)

SAMPLE = """Lab Report 2026-05-01
Hemoglobin 13.5 g/dL
WBC 6.2 10^9/L
Glucose 105 mg/dL
"""


def test_extract_returns_structured_fields():
    r = client.post("/extract", json={"document_id": 42, "text": SAMPLE})
    assert r.status_code == 200
    body = r.json()

    assert body["document_id"] == 42
    ex = body["extraction"]
    assert ex["report_type"] == "lab_report"
    assert ex["report_date"] == "2026-05-01"

    names = {i["name"] for i in ex["indicators"]}
    assert {"Hemoglobin", "WBC", "Glucose"} <= names
    hgb = next(i for i in ex["indicators"] if i["name"] == "Hemoglobin")
    assert hgb["value"] == 13.5
    assert hgb["unit"] == "g/dL"


def test_extract_reports_usage():
    r = client.post("/extract", json={"document_id": 1, "text": SAMPLE})
    usage = r.json()["usage"]
    assert usage["mode"] == "stub"  # no API key in tests
    assert usage["input_tokens"] > 0
    assert usage["est_cost_usd"] >= 0.0
    assert "latency_ms" in usage
