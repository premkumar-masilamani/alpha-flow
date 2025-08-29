# Load environment variables from .env
include .env
export $(shell sed 's/=.*//' .env)

.PHONY: run_database network database migrate_database import_seed_data connect_database run_backend run_frontend clean diagrams

# Setup database locally
run_database: network database migrate_database import_seed_data

# Create docker network if it doesn't exist
network:
	@if ! docker network inspect $(DOCKER_NETWORK_NAME) >/dev/null 2>&1; then \
		echo "Creating network: $(DOCKER_NETWORK_NAME)"; \
		docker network create --driver bridge $(DOCKER_NETWORK_NAME); \
	fi

# Start Postgres and wait until healthy
database:
	@if docker ps -a --format '{{.Names}}' | grep -Fxq $(POSTGRES_DB); then \
		echo "Removing existing $(POSTGRES_DB) container..."; \
		docker rm -f $(POSTGRES_DB) >/dev/null; \
	fi
	@echo "Starting database container..."
	docker run -d \
		--name $(POSTGRES_DB) \
		--hostname $(POSTGRES_DB) \
		--network $(DOCKER_NETWORK_NAME) \
		-p $(POSTGRES_PORT):5432 \
		-v $(LOCAL_SEED_DATA_VOLUME):$(DOCKER_SEED_DATA_VOLUME) \
		-v $(LOCAL_SCRIPTS_VOLUME):$(DOCKER_SCRIPTS_VOLUME) \
		-v $(POSTGRES_VOLUME):/var/lib/postgresql/data \
		-e POSTGRES_USER=$(POSTGRES_USER) \
		-e POSTGRES_PASSWORD=$(POSTGRES_PASSWORD) \
		-e POSTGRES_DB=$(POSTGRES_DB) \
		--health-cmd="bash $(DOCKER_SCRIPTS_VOLUME)/health_check.sh" \
		--health-interval=5s \
		--health-timeout=5s \
		--health-retries=10 \
		$(DOCKER_IMAGE_POSTGRES)
	@echo "Waiting for database to be healthy..."
	@until [ "$$(docker inspect --format='{{json .State.Health.Status}}' $(POSTGRES_DB))" = "\"healthy\"" ]; do \
		sleep 2; \
	done
	@echo "Database is healthy."

# Run database migrations
migrate_database:
	docker run --rm -i \
		-v ./database/migrations:/migrations \
		--network $(DOCKER_NETWORK_NAME) \
		$(DOCKER_IMAGE_DB_MIGRATE) \
		-path=/migrations \
		-database "postgres://${POSTGRES_USER}:${POSTGRES_PASSWORD}@$(POSTGRES_DB):$(POSTGRES_PORT)/${POSTGRES_DB}?sslmode=disable" \
		$(DATABASE_MIGRATION_DIRECTION) $(DATABASE_MIGRATION_STEP)
# Usage:
# make migrate_database DATABASE_MIGRATION_DIRECTION=up
# make migrate_database DATABASE_MIGRATION_DIRECTION=down DATABASE_MIGRATION_STEP=1

# Import seed data
import_seed_data:
	docker run --rm \
		--name import_seed_data \
		--network $(DOCKER_NETWORK_NAME) \
		-v $(LOCAL_SEED_DATA_VOLUME):$(DOCKER_SEED_DATA_VOLUME) \
		-v $(LOCAL_SCRIPTS_VOLUME):$(DOCKER_SCRIPTS_VOLUME) \
		-e POSTGRES_USER=$(POSTGRES_USER) \
		-e POSTGRES_PASSWORD=$(POSTGRES_PASSWORD) \
		-e POSTGRES_DB=$(POSTGRES_DB) \
		-e DOCKER_SEED_DATA_VOLUME=$(DOCKER_SEED_DATA_VOLUME) \
		$(DOCKER_IMAGE_POSTGRES) \
		bash $(DOCKER_SCRIPTS_VOLUME)/import_seed_data.sh

# Connect to database via psql
connect_database:
	docker exec -it $(POSTGRES_DB) psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

# Run backend without cleaning or re-downloading jars
run_backend:
	@echo "Starting backend with live reload..."
	./backend/gradlew -p ./backend bootRun --continuous --daemon --parallel --build-cache

# Run frontend in dev mode
run_frontend:
	npm --prefix ./frontend install
	npm --prefix ./frontend run dev

# Build frontend in production mode
check_frontend:
	@echo "--- Installing dependencies in frontend/ ---"
	@npm ci --prefix frontend
	@echo "--- Running security audit in frontend/ ---"
	@npm audit --audit-level=high --prefix frontend
	@echo "--- Running linting checks in frontend/ ---"
	@npm run lint --prefix frontend || true
	@echo "--- Creating production build in frontend/ ---"
	@NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL} npm run build --prefix frontend

# Clean up containers, volumes, and network
clean:
	@echo "Cleaning up containers, volumes, and network..."
	-docker rm -f $(POSTGRES_DB) >/dev/null 2>&1 || true
	-docker volume rm -f $(POSTGRES_VOLUME) >/dev/null 2>&1 || true
	-docker network rm $(DOCKER_NETWORK_NAME) >/dev/null 2>&1 || true

diagrams:
	for file in $(DIAGRAMS_DIR)/*.d2; do \
		d2 --sketch "$$file" "$${file%.d2}.svg"; \
	done
