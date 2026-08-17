"""The extraction endpoint the platform service calls (service-to-service).

Flow: platform-api stores the upload, then hands the document to this service for the slow work.
In production that handoff is a Redis job the worker consumes; this HTTP endpoint is the same logic
exposed synchronously so it is easy to call and test directly. It returns the structured extraction
plus a usage record (tokens, estimated cost, latency).
"""

from fastapi import APIRouter

from healthvault_ai import extractor
from healthvault_ai.schemas import ExtractRequest, ExtractResponse

router = APIRouter(tags=["extraction"])


@router.post("/extract", response_model=ExtractResponse)
def extract(req: ExtractRequest) -> ExtractResponse:
    extraction, usage = extractor.extract(req.text)
    return ExtractResponse(document_id=req.document_id, extraction=extraction, usage=usage)
