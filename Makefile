# Gracefully handle missing local environment configurations in CI/CD
-include .env
ifneq ($(wildcard .env),)
    export $(shell sed 's/=.*//' .env)
endif

.PHONY: all run_database connect_database run_backend run_frontend check_frontend diagrams test lint run_all

all: run_all

run_database:
	@$(MAKE) -C database all

connect_database:
	@$(MAKE) -C database connect_database

run_backend:
	@$(MAKE) -C backend dev

run_frontend:
	@$(MAKE) -C frontend dev

check_frontend:
	@echo "--- Linting Frontend ---"
	@$(MAKE) -C frontend lint
	@echo "--- Testing Frontend ---"
	@$(MAKE) -C frontend test
	@echo "--- Building Frontend ---"
	@$(MAKE) -C frontend build

test:
	@echo "--- Running Backend Tests ---"
	@$(MAKE) -C backend test
	@echo "--- Running Frontend Tests ---"
	@$(MAKE) -C frontend test

lint:
	@echo "--- Running Backend Linter ---"
	@$(MAKE) -C backend lint
	@echo "--- Running Frontend Linter ---"
	@$(MAKE) -C frontend lint

diagrams:
	@echo "Compiling D2 architecture diagrams to sketch SVG assets..."
	@if [ -d "$(DIAGRAMS_DIR)" ]; then \
		for file in $(DIAGRAMS_DIR)/*.d2; do \
			[ -e "$$file" ] || continue; \
			d2 --sketch "$$file" "$${file%.d2}.svg"; \
		done; \
	else \
		echo "Diagram directory '$(DIAGRAMS_DIR)' not found."; \
	fi

run_all:
	@echo "Step 1/2: Initializing database cluster infrastructures..."
	@$(MAKE) run_database
	@echo "Step 2/2: Spawning application instances concurrently..."
	@trap 'echo "\nShutting down environments..."; kill 0' SIGINT SIGTERM; \
	$(MAKE) run_backend & \
	$(MAKE) run_frontend & \
	wait
