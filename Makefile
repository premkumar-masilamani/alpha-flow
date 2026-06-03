# Gracefully handle missing local environment configurations in CI/CD
-include .env
ifneq ($(wildcard .env),)
    export $(shell sed 's/=.*//' .env)
endif

.PHONY: all run_database connect_database run_backend run_frontend check_frontend diagrams test lint run_all stop_all

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
	@echo "Step 2/2: Spawning application instances concurrently in the background..."
	@$(MAKE) run_backend > backend.log 2>&1 & \
	echo $$! > .backend.pid; \
	$(MAKE) run_frontend > frontend.log 2>&1 & \
	echo $$! > .frontend.pid; \
	echo "Applications spawned successfully in the background."
	@echo " - Backend log: backend.log"
	@echo " - Frontend log: frontend.log"
	@echo "Run 'make stop_all' to stop them."

stop_all:
	@echo "Stopping application instances..."
	@if [ -f .backend.pid ]; then \
		pid=$$(cat .backend.pid); \
		echo "Stopping backend process $$pid..."; \
		kill -15 $$pid 2>/dev/null || true; \
		rm -f .backend.pid; \
	fi
	@if [ -f .frontend.pid ]; then \
		pid=$$(cat .frontend.pid); \
		echo "Stopping frontend process $$pid..."; \
		kill -15 $$pid 2>/dev/null || true; \
		rm -f .frontend.pid; \
	fi
	@echo "Cleaning up port 8080 (backend)..."
	@pids=$$(lsof -t -i :8080 2>/dev/null); if [ -n "$$pids" ]; then kill -15 $$pids 2>/dev/null || true; fi
	@echo "Cleaning up port 5173 (frontend)..."
	@pids=$$(lsof -t -i :5173 2>/dev/null); if [ -n "$$pids" ]; then kill -15 $$pids 2>/dev/null || true; fi
	@echo "Stopping database container..."
	@-docker stop $$(docker ps -q --filter name=$(POSTGRES_DB)) >/dev/null 2>&1 || true
	@echo "All processes stopped successfully."
