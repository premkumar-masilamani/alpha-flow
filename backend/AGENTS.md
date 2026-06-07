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
- **Strings**: Map using `@Column(length = N)`. Always specify explicit column lengths.
- **Class naming**: Avoid names that conflict with SQL reserved words. Use alternatives (e.g., `TradingOrder` instead of `Order`).

### Enums (`persistence/enums/`)

- `IndicatorType`: `SMA`, `EMA`, `RSI`, `MACD`, `STOCHASTIC`. Stored as `VARCHAR(32)` in DB via `@Enumerated(EnumType.STRING)`.
- `Timeframe`: `DAILY`, `WEEKLY`. Stored as `VARCHAR(16)`.
- `PriceSource`: `OPEN`, `HIGH`, `LOW`, `CLOSE`, `VOLUME`. Stored as `VARCHAR(16)`.

### Repository Classes (`persistence/repositories/`)

- Extend `JpaRepository<Entity, Long>` and annotate with `@Repository`.
- Return `Optional<Entity>` for single-result lookups.
- For technical indicators, use the unified `IndicatorRepository` interface. It routes CRUD operations dynamically to package-private daily/weekly JPA repositories depending on the timeframe.
- JPQL is preferred over native SQL.

### Indicator Engine (`engine/calculators/indicators/`)

- **`Indicator` interface**: All indicators implement `compute(List<PriceBar> bars, IndicatorParams params, PriceSource source)`. Lookback limits and state resumption logic are removed.
- **`IndicatorRegistry`**: Factory that maps `IndicatorType` → concrete `Indicator` implementation.
- **`PriceBar` record**: Lightweight `record(LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume)` used as input to all indicators.
- **MathContext**: Indicator calculations use `new MathContext(18)` for `BigDecimal` arithmetic.
- **Calculations Workflow**: On each pipeline run, all prior computed indicator values for the ticker, timeframe, and active indicator definitions are completely cleared from the database via `indicatorRepository.deleteByTickerAndIndicatorIds(ticker, ids, timeframe)` and recalculated over the entire historical price list.
- **Multi-output indicators**: All computed outputs (e.g., `macd`, `signal`, `histogram` for MACD, or `k`, `d` for Stochastic) are combined into a single row per date as a key-value mapping (`Map<String, BigDecimal>`) stored in the `values` JSONB column.

### Indicator Configuration

Indicator configurations are stored in the database under `indicator_definitions` (`indicator_id`, `indicator_type`, `source`, `params`). 

Active indicators are mapped to timeframes inside `application.properties` via comma-separated indicator IDs:

```properties
alphaflow.indicators.timeframes.daily=1,2,3,4,5,6
alphaflow.indicators.timeframes.weekly=7
```

- **In-Memory Cache**: `IndicatorConfig` loads all definitions from the database at startup (`@PostConstruct`), mapping and caching them by timeframe.
- **Strict Validation**: The config warns at startup if any indicator ID specified in the properties is not found in the database.

### Scheduler (`engine/schedulers/`)

- `CoreScheduler` orchestrates the data pipeline: download daily prices → compute weekly prices → compute indicators.
- Runs on startup via `@EventListener(ApplicationReadyEvent.class)` and hourly via `@Scheduled(cron = "0 0 * * * *")`.

### Downloader (`engine/downloaders/`)

- Uses Java `HttpClient` (no third-party HTTP libraries).
- Rate-limited via `alphaflow.yahoo.delay-milliseconds` (default 1000ms between requests).
- **Data Integrity / Zero Price Filter**: The downloader discards any parsed price bars where `open`, `high`, `low`, or `close` price is less than or equal to `0`. This filters out mid-day or incomplete Yahoo Finance chart returns containing zero prices.
- All config externalized to `application.properties` via `YahooFinanceConfig` (bound with prefix `alphaflow.yahoo`).
- Timestamps from Yahoo Finance are converted to `LocalDate` using the ticker's timezone.

---

## Project Boundaries

### Always Do
- Use `BigDecimal` for all prices, monetary values, and volume. Never `double` or `float`.
- Use `Long` for all IDs. Use `LocalDate` for dates, `LocalDateTime` for timestamps.
- Compile after code changes: `./backend/gradlew -p ./backend compileJava`.
- Keep PK and FK types consistent (`INT8` / `BIGINT` everywhere).

### Ask First
- Modifying scheduled core jobs or the downloader frequency/rate-limiting logic.
- Adding third-party libraries/dependencies to `build.gradle`.

### Never Do
- Do not bypass `BigDecimal` for price data types.
- Do not use JPA `ddl-auto` to generate database schemas directly.
- Do not omit `@Column(length = N)` on `String` entity fields — always match the DB `VARCHAR(N)`.
- Do not omit `precision` and `scale` on `BigDecimal` entity fields — always match the DB `NUMERIC(P, S)`.
- Do not use `INT` for primary keys or foreign keys — always use `INT8` / `BIGINT`.

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
