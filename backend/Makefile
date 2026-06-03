-include ../.env
ifneq ($(wildcard ../.env),)
    export $(shell sed 's/=.*//' ../.env)
endif

GRADLE := ./gradlew
DEV_FLAGS   := --daemon --parallel --build-cache --configuration-cache
PROD_FLAGS  := --no-daemon --parallel --build-cache --configuration-cache

.PHONY: dev test coverage lint build format

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
	@echo "Executing comprehensive Java static analysis suite with auto-fixes..."
	@echo "1/4 Applying Spotless code formatting and Checkstyle fixes..."
	$(GRADLE) spotlessApply $(DEV_FLAGS)
	@echo "2/4 Verifying remaining Checkstyle formatting rules..."
	$(GRADLE) checkstyleMain checkstyleTest $(DEV_FLAGS)
	@echo "3/4 Running PMD code smell scan..."
	$(GRADLE) pmdMain pmdTest $(DEV_FLAGS)
	@echo "4/4 Running SpotBugs bytecode vulnerability sweep..."
	$(GRADLE) spotbugsMain spotbugsTest $(DEV_FLAGS)
	@echo "Code verification and auto-fixing complete."

format:
	@echo "Reformatting Java codebase using Spotless..."
	$(GRADLE) spotlessApply $(DEV_FLAGS)

build:
	@echo "Building production artifact (running full verification)..."
	$(GRADLE) clean build $(PROD_FLAGS)
