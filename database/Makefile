include ../.env
export $(shell sed 's/=.*//' ../.env)

.PHONY: run_database network database migrate_database import_seed_data connect_database clean

run_database: network database migrate_database import_seed_data

network:
	@if ! docker network inspect $(DOCKER_NETWORK_NAME) >/dev/null 2>&1; then \
		echo "Creating network: $(DOCKER_NETWORK_NAME)"; \
		docker network create --driver bridge $(DOCKER_NETWORK_NAME); \
	fi

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
		-v $(abspath ./data):$(DOCKER_SEED_DATA_VOLUME) \
		-v $(abspath ./scripts):$(DOCKER_SCRIPTS_VOLUME) \
		-v $(DOCKER_POSTGRES_VOLUME):/var/lib/postgresql/data \
		-e POSTGRES_USER=$(POSTGRES_USER) \
		-e POSTGRES_PASSWORD=$(POSTGRES_PASSWORD) \
		-e POSTGRES_DB=$(POSTGRES_DB) \
		--health-cmd="bash $(DOCKER_SCRIPTS_VOLUME)/health_check.sh" \
		--health-interval=5s \
		--health-timeout=5s \
		--health-retries=10 \
		$(DOCKER_IMAGE_POSTGRES) \
		-c shared_preload_libraries=pg_stat_statements
	@echo "Waiting for database to be healthy..."
	@until [ "$$(docker inspect --format='{{json .State.Health.Status}}' $(POSTGRES_DB))" = "\"healthy\"" ]; do \
		sleep 2; \
	done
	@echo "Database is healthy."

migrate_database:
	docker run --rm -i \
		-v $(abspath ./migrations):/migrations \
		--network $(DOCKER_NETWORK_NAME) \
		$(DOCKER_IMAGE_DB_MIGRATE) \
		-path=/migrations \
		-database "postgres://${POSTGRES_USER}:${POSTGRES_PASSWORD}@$(POSTGRES_DB):5432/${POSTGRES_DB}?sslmode=disable" up

import_seed_data:
	docker run --rm \
		--name import_seed_data \
		--network $(DOCKER_NETWORK_NAME) \
		-v $(abspath ./data):$(DOCKER_SEED_DATA_VOLUME) \
		-v $(abspath ./scripts):$(DOCKER_SCRIPTS_VOLUME) \
		-e POSTGRES_USER=$(POSTGRES_USER) \
		-e POSTGRES_PASSWORD=$(POSTGRES_PASSWORD) \
		-e POSTGRES_DB=$(POSTGRES_DB) \
		-e DOCKER_SEED_DATA_VOLUME=$(DOCKER_SEED_DATA_VOLUME) \
		$(DOCKER_IMAGE_POSTGRES) \
		bash $(DOCKER_SCRIPTS_VOLUME)/import_seed_data.sh

connect_database:
	docker exec -it $(POSTGRES_DB) psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

clean:
	@echo "You are about to delete the following resources:"
	@echo "  - Container: $(POSTGRES_DB)"
	@echo "  - Volume:    $(DOCKER_POSTGRES_VOLUME)"
	@echo "  - Network:   $(DOCKER_NETWORK_NAME)"
	@echo ""
	@printf "Are you sure you want to delete these? (yes/no): " && read ans && [ "$$ans" = "yes" ] || (echo "Aborted." && exit 1)
	@printf "Please type 'delete' to confirm irreversible removal: " && read ans && [ "$$ans" = "delete" ] || (echo "Aborted." && exit 1)
	@echo "Cleaning up containers, volumes, and network..."
	-docker rm -f $(POSTGRES_DB) >/dev/null 2>&1 || true
	-docker volume rm -f $(DOCKER_POSTGRES_VOLUME) >/dev/null 2>&1 || true
	-docker network rm $(DOCKER_NETWORK_NAME) >/dev/null 2>&1 || true
	@echo "Cleanup completed."
