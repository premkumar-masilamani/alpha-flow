# AGENTS.md - Backend

Operational guidance for AI coding agents working on the Spring Boot backend (`backend/` subdirectory).

## Setup & Running Commands

All backend operations are orchestrated using the module `Makefile` inside the `backend/` directory.

### Commands

| Command | Action / Behind the Scenes |
| :--- | :--- |
| `make dev` | Starts the Spring Boot application in development mode with continuous live hot-reloading (`bootRun --continuous` using dev build caching daemon settings). |
| `make test` | Runs the full JUnit test suite using Gradle daemon settings. |
| `make coverage` | Runs tests, generates the JaCoCo HTML coverage report, and automatically opens it in your default browser (macOS/Linux only, skips in headless environments). |
| `make lint` | Runs the full 4-stage static analysis pipeline (Spotless formatting, Checkstyle validation, PMD scanning, SpotBugs vulnerability sweep). |
| `make format` | Reformats all Java codebase files using Spotless (`spotlessApply`). |
| `make build` | Performs a clean production build (`clean build --no-daemon`) with all verification checks enabled. |

### Build Flags Configurations
The `Makefile` defines two flag sets passed to Gradle:
- **`DEV_FLAGS`**: `--daemon --parallel --build-cache --configuration-cache` (used for `dev`, `test`, `coverage`, `lint`, and `format` to optimize speed).
- **`PROD_FLAGS`**: `--no-daemon --parallel --build-cache --configuration-cache` (used for `build` to ensure clean, isolated builds without holding daemon processes).

---

## Backend Architecture

The backend is structured under the `com.alphaflow` package:

- `com.alphaflow.api` — REST API layer containing:
  - `configs/` — API-specific configs (CORS, thread pools, doc properties).
  - `controllers/` — REST controllers exposing endpoints.
  - `services/` — Business logic and transaction boundaries.
  - `dtos/` — Data transfer objects.
  - `mappers/` — DTO/Entity converters.
  - `utils/` — REST API helpers.
- `com.alphaflow.engine` — Computation & scheduler layer containing:
  - `configs/` — Properties/Configs for Yahoo Finance and Indicators.
  - `downloaders/` — Yahoo Finance data fetching mechanisms.
  - `calculators/` — Processors for computing weekly prices and technical indicators.
  - `schedulers/` — Orchestrator jobs running on startup and hourly intervals.
- `com.alphaflow.persistence` — Persistence & Database layer containing:
  - `entities/` — JPA database models mapped to Postgres tables.
  - `repositories/` — Spring Data JPA repositories.
  - `enums/` — Shared enums (e.g., `IndicatorType`, `Timeframe`, `PriceSource`).
  - `exceptions/` — Persistence-related exceptions.

---

## Coding Conventions & Constraints

- **API Organization**: All REST-specific code must reside in the `api/` package.
- **Service Responsibility**: The service layer in `api/services/` coordinates business tasks and transactions. Direct DB query manipulation or multiple consecutive calls to repositories should generally reside inside or be encapsulated by repositories in `persistence/`.
- **Scheduled Tasks**: Background calculation engines, downloader components, and schedulers belong in `engine/`.
- **Database Operations**: Anything directly related to connecting or managing DB models is stored in `persistence/`.

### Spring Boot & Java Style

- **Dependency Injection**: Prefer constructor-based injection (manual constructors or Lombok `@RequiredArgsConstructor`) over field-level `@Autowired` (field injection should be avoided unless required for specific self-proxy cyclic resolution like `@Autowired @Lazy private AnalysisService self;`).
- **Boilerplate Reduction**: Use Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, etc.) on Entities, DTOs, and configs.
- **Global Error Handling**: Centralized in `@RestControllerAdvice` under `com.alphaflow.api.controllers.generic.GlobalExceptionHandler`.

### JPA & Entity Mapping Rules

When defining or modifying JPA Entity classes under `persistence/entities/`, ensure they strictly adhere to:
- **Class Annotations**: Every entity must carry these annotations:
  ```java
  @Entity
  @Table(name = "table_name")
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  ```
- **Primary Keys**: Always mapped as `Long` object type with `@GeneratedValue(strategy = GenerationType.IDENTITY)` and annotated with `@EqualsAndHashCode.Include`.
- **Relationship Fetching & Recursion**: Use lazy fetching (`fetch = FetchType.LAZY`) for `@ManyToOne` and `@OneToMany` relationships. Add `@ToString(exclude = "fieldName")` on the entity to prevent infinite recursion / N+1 logs when rendering `toString()`.
- **Default Values**: Use Lombok `@Builder.Default` on fields with defaults (e.g., `private String currency = "USD"`).
- **BigDecimal Details**: Always define exact database alignment with `@Column(precision = 18, scale = 4)` for all prices, indicators, and volume fields.
- **Enums**: Map using `@Enumerated(EnumType.STRING)` with a specified `@Column(length = N)` to avoid default length issues.
- **JSONB**: For opaque/internal state representations, use `@JdbcTypeCode(SqlTypes.JSON)` with `columnDefinition = "jsonb"`.
- **Strings**: Map using `@Column(length = N)`. Always specify explicit column lengths.

### Data Types & Calculations

- **BigDecimal Mandatory**: All pricing, volumes, calculations, indicators, and monetary values must strictly use `BigDecimal`. Never use `double` or `float` primitive/boxed types.
- **Math Precision**: Perform indicator mathematics using `new MathContext(18)` for precise division and rounding.

### Schema Management

- Hibernate `ddl-auto=none` is enforced. All schema mutations must be written as raw SQL migration scripts under `database/migrations/` and run using the migration pipeline.
- Ensure that Entity columns (`name`, `length`, `precision`, `scale`, `nullable`) match their matching database migration columns exactly.

---

## Formatting & Linting Instructions

Formatting and code style are strictly checked via the static analysis suite. Any style or structural warning will fail build pipelines.

### Code Formatting (Spotless)

- **Standard**: Formatting is strictly managed by **Spotless** using the **Google Java Format (v1.17.0)**.
- **Indentation**: 2 spaces (no tabs).
- **Imports**: Google Java Style import order rules (alphabetical grouping).
- **Braces**: Curly braces are mandatory for all control flows (`if`, `else`, `for`, `while`), regardless of line length.
- **Command**: Run format auto-fixing before committing:
  ```bash
  make format
  ```

### Static Analysis (Checkstyle, PMD, SpotBugs)

Run all validation checks using:
```bash
make lint
```
The task compiles and scans the code in four parallelizable stages:
1. **Spotless Formatting & Fixes**: Fixes basic syntax spacing/tabs/imports.
2. **Checkstyle**: Validates additional layout and code-style policies using Google checkstyle configurations (`config/checkstyle/google_checks.xml`).
3. **PMD**: Performs code smell scans using custom ruleset (`config/pmd/pmd-ruleset.xml`).
4. **SpotBugs**: Performs bytecode audits looking for bug patterns or security flaws with maximum effort and medium report level using filters (`config/spotbugs/excludeFilter.xml`).

*Note: Reports are output as interactive HTML files under `build/reports/` for simple dashboard debugging.*

### Testing & Code Coverage (JaCoCo)

- **100% Coverage Target**: The project enforces strict verification via `jacocoTestCoverageVerification`. All non-excluded packages MUST maintain 100% line and branch coverage.
- **Exclusion Packages**:
  - `com.alphaflow.api.dtos.*`
  - `com.alphaflow.api.mappers.*`
  - `com.alphaflow.persistence.entities.*`
  - `com.alphaflow.persistence.enums.*`
  - `com.alphaflow.persistence.exceptions.*`
  - `com.alphaflow.persistence.repositories.*`
- **Unit Testing requirement**: Code in controller endpoints (`api/controllers/`), service handlers (`api/services/`), and computation logic (`engine/`) must have corresponding unit test suites covering edge cases.
- **Verification Commands**:
  - Run all tests: `make test`
  - Run tests and open coverage page: `make coverage`

---

## AI Learnings & Coding Guidelines

### JPQL Date Literals (Hibernate 6)
- **Always use the standard JPA/JDBC date escape syntax `{d 'yyyy-MM-dd'}`** (e.g., `{d '1900-01-01'}`) when hardcoding date literals in JPQL `@Query` annotations. Avoid dialect-specific strings like `date 'yyyy-MM-dd'` which are rejected by Hibernate 6.

### Decoupled Parser and Network Boundaries
- Keep connection/fetching logic (like opening HTTP streams) separate from parser/deserialization logic. Inject parser components (e.g., `YahooResponseParser`) to process fetched strings, enabling isolated testing of serialization formats.

### Nesting DTO Java Records
- Inner records are **implicitly static**. Do not add the `static` modifier to nested records (e.g., use `private record YahooResponse(...) {}` instead of `private static record YahooResponse(...) {}`) to avoid PMD static analysis violations (`UnnecessaryModifier`).

### SpotBugs and Stream Closure
- Always wrap resource-acquiring methods (like `getClass().getResourceAsStream(...)`) in **try-with-resources** blocks. This ensures streams are cleaned up and prevents SpotBugs resource leaks (`OS_OPEN_STREAM`, `OBL_UNSATISFIED_OBLIGATION`).

### Spring AOP Self-Proxy Fallback
- When injecting a self-referential Spring bean proxy (`@Autowired @Lazy private MyService self`) to invoke `@Transactional` methods internally, always implement a fallback to `this` when `self` is null (e.g., `MyService proxy = (self != null) ? self : this;`). This ensures the code remains fully testable in unit tests without a Spring context.

### Bulk Data Querying (Avoiding N+1 Query Patterns)
- Avoid query operations in a loop (e.g., retrieving the first/last date per-ticker). Instead, fetch the required attributes in a single bulk query (e.g., returning a `Map<Ticker, LocalDate>`) and iterate over the map.

### Stream Aggregators for Extremum Finding
- Prefer Java Stream API operations (e.g., `stream().max(BigDecimal::compareTo)` and `stream().min(BigDecimal::compareTo)`) over manual `for` loops when finding minimum/maximum values of lists to write clean, declarative Java code.


