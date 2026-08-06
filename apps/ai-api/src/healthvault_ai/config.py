from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "ai-api"
    redis_url: str = "redis://localhost:6379/0"
    database_url: str = "postgresql://healthvault:devpassword@localhost:5432/healthvault"


settings = Settings()
