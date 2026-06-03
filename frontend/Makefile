NODE_ENV    ?= development
NPM         := npm

.PHONY: all install dev build preview lint audit test check clean help

# Default target
all: help

# Enforced npm ci for strict determinism across all environments
install:
	@echo "Installing deterministic dependencies via npm ci..."
	$(NPM) ci

dev: install
	@echo "Starting development environment..."
	$(NPM) run dev

build: install
	@echo "Building production bundles (NODE_ENV=$(NODE_ENV))..."
	$(NPM) run build

preview:
	@echo "Previewing the production build local server..."
	$(NPM) run preview

lint: install
	@echo "Running static analysis code verification..."
	$(NPM) run lint

audit:
	@echo "Auditing production dependencies for high/critical security risks..."
	$(NPM) audit --omit=dev --audit-level=high

test: install
	@echo "Running unit tests using Vitest..."
	npx vitest run

check:
	@$(MAKE) install
	@$(MAKE) audit
	@$(MAKE) lint
	@$(MAKE) test
	@$(MAKE) build

clean:
	@echo "Cleaning up local build workspaces and dependency trees..."
	rm -rf node_modules dist .nuxt .next .svelte-kit
	@echo "Workspace cleaned. Run 'make install' to regenerate node_modules from lockfile."

help:
	@echo "========================================================================"
	@echo "Production-Grade Frontend Management Utilities (Enforced npm ci)"
	@echo "========================================================================"
	@echo "  make install  - Strictly install dependencies using package-lock.json"
	@echo "  make dev      - Spin up local development infrastructure"
	@echo "  make build    - Compile optimized production code distribution assets"
	@echo "  make preview  - Boot local wrapper to check compiled bundle assets"
	@echo "  make lint     - Execute ESLint validation maps"
	@echo "  make audit    - Run strict production security validation scripts"
	@echo "  make test     - Run unit tests with Vitest"
	@echo "  make check    - Comprehensive sequential validation workflow execution"
	@echo "  make clean    - Wipe local build caches and workspace nodes"
