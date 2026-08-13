-include ../.env
ifneq ($(wildcard ../.env),)
    export $(shell sed 's/=.*//' ../.env)
endif

NPM         := npm
NODE_ENV    ?= development
export NODE_OPTIONS=--max-old-space-size=4096

.PHONY: dev test lint build clean

all: dev

dev:
	@echo "Starting development environment..."
	$(NPM) run dev

test:
	@echo "Running unit tests using Vitest..."
	npx vitest run

lint:
	@echo "Running static analysis code verification..."
	$(NPM) run lint

build:
	@echo "Building production bundles (NODE_ENV=$(NODE_ENV))..."
	$(NPM) run build

clean:
	@echo "Cleaning up local build workspaces..."
	rm -rf dist
