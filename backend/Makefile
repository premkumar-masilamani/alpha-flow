-include ../.env
ifneq ($(wildcard ../.env),)
    export $(shell sed 's/=.*//' ../.env)
endif

GRADLE := ./gradlew
DEV_FLAGS   := --daemon --parallel --build-cache --configuration-cache
PROD_FLAGS  := --no-daemon --parallel --build-cache --configuration-cache

.PHONY: dev test coverage build

all: dev

dev:
	@echo "Running application in dev mode with hot-reloading..."
	$(GRADLE) bootRun --continuous $(DEV_FLAGS)

test:
	@echo "Running unit tests..."
	$(GRADLE) test $(DEV_FLAGS)

coverage: test
	@echo "Generating JaCoCo coverage report..."
	$(GRADLE) jacocoTestReport $(DEV_FLAGS)
	@echo "Checking environment to open coverage report..."
	@if [ -t 0 ]; then \
		if [ "$$(uname)" = "Darwin" ]; then \
			open build/reports/jacoco/test/html/index.html 2>/dev/null || true; \
		elif [ "$$(uname -s)" = "Linux" ]; then \
			xdg-open build/reports/jacoco/test/html/index.html 2>/dev/null || true; \
		fi; \
	else \
		echo "Headless or non-interactive terminal detected. Skipping browser launch."; \
	fi

lint:
	@echo "Executing comprehensive Java static analysis suite..."
	@echo "1/3 running Checkstyle formatting audit..."
	$(GRADLE) checkstyleMain checkstyleTest $(DEV_FLAGS)
	@echo "2/3 running PMD code smell scan..."
	$(GRADLE) pmdMain pmdTest $(DEV_FLAGS)
	@echo "3/3 running SpotBugs bytecode vulnerability sweep..."
	$(GRADLE) spotbugsMain spotbugsTest $(DEV_FLAGS)
	@echo "Code verification passed cleanly."

build:
	@echo "Building production artifact (running full verification)..."
	$(GRADLE) clean build $(PROD_FLAGS)
