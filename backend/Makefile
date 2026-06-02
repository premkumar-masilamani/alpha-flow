.PHONY: dev test build compile clean

dev:
	./gradlew bootRun --continuous --daemon --parallel --build-cache

test:
	@echo "Running backend unit tests and generating JaCoCo coverage report..."
	./gradlew clean test; \
	status=$$?; \
	open build/reports/jacoco/test/html/index.html; \
	exit $$status

build:
	./gradlew build -x test

compile:
	./gradlew compileJava

clean:
	./gradlew clean