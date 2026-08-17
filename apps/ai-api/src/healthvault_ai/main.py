from fastapi import FastAPI

from healthvault_ai.config import settings
from healthvault_ai.routers import extract, health

app = FastAPI(title=settings.app_name)
app.include_router(health.router)
app.include_router(extract.router)


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "ai-api", "hello": "world"}
