"""Structured output contract.

The LLM does not return free text; it must return JSON that matches ``LabExtraction``. We hand the
model this shape as a JSON Schema so the fields, types, and enums are guaranteed, and we parse the
reply straight into these Pydantic models. Same schema drives the real API call and validates the
offline stub, so both paths produce identical, typed results.
"""

from __future__ import annotations

from datetime import date
from typing import Literal

from pydantic import BaseModel, Field


class Indicator(BaseModel):
    name: str = Field(description="indicator name, e.g. Hemoglobin")
    value: float
    unit: str | None = Field(default=None, description="unit, e.g. g/dL")
    status: Literal["normal", "high", "low"] | None = None


class LabExtraction(BaseModel):
    report_type: str = Field(description="e.g. lab_report, prescription")
    report_date: date | None = None
    indicators: list[Indicator] = Field(default_factory=list)


class ExtractRequest(BaseModel):
    document_id: int
    # The document's text (OCR'd upstream). Kept simple here to focus on the extraction contract.
    text: str


class Usage(BaseModel):
    mode: Literal["openai", "stub"]
    model: str
    input_tokens: int
    output_tokens: int
    est_cost_usd: float
    latency_ms: int


class ExtractResponse(BaseModel):
    document_id: int
    extraction: LabExtraction
    usage: Usage
