.PHONY: test

# Run all backend unit tests and generate JaCoCo code coverage report
test:
	@echo "Running backend unit tests and generating JaCoCo coverage report..."
	./gradlew clean test; \
	status=$$?; \
	open build/reports/jacoco/test/html/index.html; \
	exit $$status



#	open build/reports/tests/test/index.html; \