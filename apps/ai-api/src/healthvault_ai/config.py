from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "ai-api"
    redis_url: str = "redis://localhost:6379/0"
    database_url: str = "postgresql://healthvault:devpassword@localhost:5432/healthvault"

    # LLM extraction. With no key the extractor falls back to a deterministic stub, so the
    # service (and its tests) run offline with no cost. Set OPENAI_API_KEY to use the real model.
    openai_api_key: str = ""
    extraction_model: str = "gpt-4o-mini"


settings = Settings()
