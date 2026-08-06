.PHONY: dev up down ai platform test

up:            ## start postgres + redis
	docker compose -f infra/docker-compose.yml up -d

down:
	docker compose -f infra/docker-compose.yml down

ai:            ## run the FastAPI AI service
	cd apps/ai-api && PYTHONPATH=src fastapi dev src/healthvault_ai/main.py

platform:      ## run the Spring Boot platform service
	cd apps/platform-api && ./gradlew bootRun

test:
	cd apps/ai-api && PYTHONPATH=src pytest -q
	cd apps/platform-api && ./gradlew test --no-daemon
