# AGENTS.md - Backend

Operational guidance for AI coding agents working on the Spring Boot backend (`backend/` subdirectory).

## Setup & Running Commands

Run all operations using the module `Makefile` inside the `backend/` directory:

```bash
make dev             # Start the Spring Boot application (live reload continuous)
make test            # Run all unit tests and generate JaCoCo coverage report
make build           # Build the project, skipping tests
make compile         # Run compilation/type-safety checks
make clean           # Clean build outputs
```

## Backend Architecture

The backend is structured under the `com.alphaflow` package:

- `api/` — Controllers (`TickerController`, `IndicatorController`, `DailyPriceController`, etc.), mappers, DTOs, configs, global handlers.
- `engine/` — Scheduled jobs (`CoreScheduler`), data downloader (`YahooFinanceDownloader`), indicator processors/calculators (`WeeklyPriceCalculator`, `IndicatorCalculator`).
- `persistence/` — JPA Entities (`Ticker`, `DailyPrice`, `WeeklyPrice`, `IndicatorState`, etc.), repositories, enums, exceptions.

## Coding Conventions & Constraints

### Spring Boot & Java Style

- Use constructor-based dependency injection. Avoid `@Autowired` on fields.
- Use Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, etc.) to eliminate boilerplate in entities, DTOs, and configs.
- Implement proper global exception handling via `@RestControllerAdvice` in `com.alphaflow.api.controllers.generic.GlobalExceptionHandler`.

### Data Types & Calculations

- **Always** use `BigDecimal` for price fields (`numeric(18,4)`) and `Long` for volume fields (`bigint`).
- **Never** introduce `float` or `double` into calculations or entity attributes representing money or price values to avoid rounding issues.

### Schema Ownership

- Hibernate `ddl-auto=none` is enabled. The DB schema is strictly managed by SQL migrations in `database/migrations/`.
- When updating an entity, **always** ensure a matching database migration exists.

## Formatting & Linting Instructions

### Code Formatting

- **Indentation**: 4 spaces. Do not use tabs.
- **Line Length**: Max 120 characters.
- **Organize Imports**: Group imports alphabetically: static imports first, then standard java/javax package libraries, then third-party libraries, then internal `com.alphaflow` imports.
- **Curly Braces**: Always use braces for control flow statements (`if`, `else`, `for`, `while`), even if they are single-line.
- **Annotation Placement**: In Entity and DTO classes, place class-level Lombok and Spring annotations on separate lines.

### Compilation & Static Analysis

- **Code Compilation**: Run `make compile` in the `backend/` directory to make sure there are no warnings or errors before committing.
- **Compiler Warnings**: Avoid compiler warnings. Add `@SuppressWarnings` sparingly and only with a comment explaining why.

### Testing & Code Coverage (JaCoCo)

- **Coverage Rules**: The project uses JaCoCo for verification (`jacocoTestCoverageVerification`). Non-excluded packages MUST have 100% line and branch coverage.
- **Excluded packages**:
    - `com.alphaflow.api.dtos.*`
    - `com.alphaflow.api.mappers.*`
    - `com.alphaflow.persistence.entities.*`
    - `com.alphaflow.persistence.enums.*`
    - `com.alphaflow.persistence.exceptions.*`
    - `com.alphaflow.persistence.repositories.*`
- **Strict Verification**: Code changes in service layers (`api/services/`), calculation engines (`engine/`), and controller endpoints (`api/controllers/`) MUST have complete unit tests.
- Verify tests and coverage via:
  ```bash
  make test
  ```

