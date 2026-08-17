"""Turn a document's text into a typed ``LabExtraction``.

Two interchangeable paths behind one function:

* real: call the model with the JSON Schema as the required response format, so the reply parses
  straight into the Pydantic model. Token counts come from the API's own usage report.
* stub: a deterministic regex parser used when no API key is set, so the service runs offline with
  no cost and tests are stable. Same output type as the real path.
"""

from __future__ import annotations

import re
import time

from healthvault_ai import cost
from healthvault_ai.config import settings
from healthvault_ai.schemas import Indicator, LabExtraction, Usage

_DATE = re.compile(r"\b(\d{4}-\d{2}-\d{2})\b")
# "<name> <number> <unit>", e.g. "Hemoglobin 13.5 g/dL"
_ROW = re.compile(r"^([A-Za-z][A-Za-z ]+?)\s+(\d+(?:\.\d+)?)\s*([%A-Za-z/^0-9]*)$")

_SYSTEM = (
    "Extract structured fields from this medical document. "
    "Return only values present in the text; do not invent readings."
)


def extract(text: str) -> tuple[LabExtraction, Usage]:
    started = time.perf_counter()
    if settings.openai_api_key:
        extraction, in_tok, out_tok = _extract_openai(text)
        mode = "openai"
    else:
        extraction, in_tok, out_tok = _extract_stub(text)
        mode = "stub"
    latency_ms = int((time.perf_counter() - started) * 1000)

    usage = Usage(
        mode=mode,
        model=settings.extraction_model,
        input_tokens=in_tok,
        output_tokens=out_tok,
        est_cost_usd=cost.estimate_cost(settings.extraction_model, in_tok, out_tok),
        latency_ms=latency_ms,
    )
    return extraction, usage


def _extract_openai(text: str) -> tuple[LabExtraction, int, int]:
    from openai import OpenAI  # lazy: only needed on the real path

    client = OpenAI(api_key=settings.openai_api_key)
    # Structured Outputs: the schema is enforced by the API, the reply parses into LabExtraction.
    completion = client.beta.chat.completions.parse(
        model=settings.extraction_model,
        messages=[
            {"role": "system", "content": _SYSTEM},
            {"role": "user", "content": text},
        ],
        response_format=LabExtraction,
    )
    extraction = completion.choices[0].message.parsed or LabExtraction(report_type="unknown")
    usage = completion.usage
    return extraction, usage.prompt_tokens, usage.completion_tokens


def _extract_stub(text: str) -> tuple[LabExtraction, int, int]:
    report_date = None
    match = _DATE.search(text)
    if match:
        report_date = match.group(1)

    indicators: list[Indicator] = []
    for line in text.splitlines():
        row = _ROW.match(line.strip())
        if not row:
            continue
        name = row.group(1).strip()
        if name.lower() in {"lab report", "report", "date"}:
            continue
        indicators.append(
            Indicator(name=name, value=float(row.group(2)), unit=row.group(3) or None)
        )

    extraction = LabExtraction(
        report_type="lab_report", report_date=report_date, indicators=indicators
    )
    # No real API call, so approximate the tokens the model would have seen and produced.
    return extraction, cost.estimate_tokens(text), cost.estimate_tokens(extraction.model_dump_json())
