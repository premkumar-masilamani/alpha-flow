# Load environment variables from .env
include .env
export $(shell sed 's/=.*//' .env)

.PHONY: run_database migrate_database connect_database run_backend run_frontend check_frontend clean diagrams test run_all

run_database:
	make -C database run_database

migrate_database:
	make -C database migrate_database

connect_database:
	make -C database connect_database

run_backend:
	make -C backend dev

run_frontend:
	make -C frontend install
	make -C frontend dev

check_frontend:
	make -C frontend check

test:
	make -C backend test

clean:
	make -C database clean

diagrams:
	for file in $(DIAGRAMS_DIR)/*.d2; do \
		d2 --sketch "$$file" "$${file%.d2}.svg"; \
	done

run_all: run_database
	@echo "Starting backend and frontend concurrently..."
	@trap 'kill 0' SIGINT; \
	make run_backend & \
	make run_frontend & \
	wait