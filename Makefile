# Gracefully handle missing local environment configurations in CI/CD
-include .env
ifneq ($(wildcard .env),)
    export $(shell sed 's/=.*//' .env)
endif

.PHONY: all run_database connect_database run_backend run_frontend test lint diagrams generate_diagram check_separation

all: run_backend

run_database:
	@echo "Starting the database..."
	@$(MAKE) -C database all

connect_database:
	@echo "Connecting to the database CLI..."
	@$(MAKE) -C database connect_database

run_backend:
	@echo "Launching backend development server..."
	@$(MAKE) -C backend dev

run_frontend:
	@echo "Launching frontend development server..."
	@$(MAKE) -C frontend dev

test:
	@echo "Running backend test suites..."
	@$(MAKE) -C backend test
	@echo "Running frontend test suites..."
	@$(MAKE) -C frontend test

lint:
	@echo "Linting backend source code..."
	@$(MAKE) -C backend lint
	@echo "Linting frontend source code..."
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

generate_diagram:
	@echo "Generating Java backend class map and interactive visualizer..."
	@python3 architecture/diagrams/generate_diagram.py

check_separation:
	@echo "Verifying backend package separation boundaries..."
	@python3 architecture/diagrams/generate_diagram.py --check

