# Makefile for React application

.PHONY: install dev build preview lint clean audit check help

install:
	npm install

dev:
	npm run dev

build:
	npm run build

preview:
	npm run preview

lint:
	npm run lint

audit:
	npm audit --audit-level=high

check: install audit lint build

clean:
	rm -rf node_modules dist

help:
	@echo "Available targets:"
	@echo "  make install  - Install dependencies"
	@echo "  make dev      - Run Vite dev server"
	@echo "  make build    - Create production build"
	@echo "  make preview  - Preview production build"
	@echo "  make lint     - Run ESLint"
	@echo "  make audit    - Run npm audit"
	@echo "  make check    - Run audit, lint, and build checks"
	@echo "  make clean    - Remove node_modules and dist"
