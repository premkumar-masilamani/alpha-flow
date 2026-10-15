# AGENTS.md - Backend

## Commands
```bash
# Start Spring Boot app in dev mode (hot-reloading)
make dev

# Run full JUnit test suite
make test

# Run tests + generate JaCoCo coverage report
make coverage

# Run formatting, Checkstyle, PMD, and SpotBugs
make lint

# Reformat all Java codebase files using Spotless
make format
```

## Boundaries

### Always do
- Use `BigDecimal` for prices, monetary values, and volume.
- Use `Long` for all IDs and `LocalDate` / `LocalDateTime` for dates.
- Enforce strict boundaries: `api` depends on `engine`, `persistence`, and `common`; `engine` depends on `persistence` and `common`; `persistence` depends on `common`; `common` is completely self-contained and can be used by all layers.
- Ensure all Java files contain exactly one Java type definition (only one class, record, interface, or enum per file) with no nested or extra package-private helper type definitions.
- Ensure all code (including tests and newly generated files) fully conforms to Checkstyle, PMD, and Spotless formatting rules. Fix code quality warnings in the source code rather than suppressing them.
- Explicitly branch on `Timeframe`: always use `if (timeframe == Timeframe.DAILY)` followed by `else if (timeframe == Timeframe.WEEKLY)`. The terminal `else` block must explicitly log an error (`log.error(...)`) and throw `new IllegalArgumentException("Unsupported timeframe: " + timeframe)`.
- Use descriptive variable and parameter names: Always use full, readable domain names (e.g. `bucket` instead of `b`, `bar` instead of `b`, `dailyPrice` / `weeklyPrice` instead of `d` / `w`, `pivot` instead of `p`, `bucketWidth` instead of `w`, and descriptive lambda parameters like `match`, `candle`, `record`). Standard loop counters (`i`, `j`, `k`) are permitted for indexed loops.
- Structure conditional branching so the `if` block executes the primary business logic (happy path), while the `else` block handles error, warning, fallback, or insufficient data conditions (the `else` portion should handle the error/warning conditions; the `if` condition should do the actual logic).

### Ask first
- Adding third-party libraries/dependencies to `build.gradle`.

### Never do
- Bypass `BigDecimal` for price data types (never use `double` or `float`).
- Use JPA `ddl-auto` to generate schemas.
- Omit `@Column(length = N)` on Strings or `precision` / `scale` on BigDecimals.
- Use a fallback `else` or ternary default for `Timeframe` branches without explicit validation.
- Use cryptic or single-letter variable names (such as `b`, `d`, `w`, `p`, `v`, `r`) for domain models, method parameters, or stream/lambda expressions.
- Place error, warning, or insufficient data handling inside the `if` block while relegating the actual business logic to the `else` block.
- Define default values for configuration variables or request parameters unless specifically mentioned.

## Project Structure
```text
src/main/java/com/alphaflow/api/         # REST API layer (controllers, services, DTOs)
src/main/java/com/alphaflow/engine/      # Computation (downloaders, calculators, schedulers)
src/main/java/com/alphaflow/persistence/ # DB layer (JPA entities, repositories, enums)
src/main/java/com/alphaflow/common/      # Shared cross-cutting constants and enums
```

## Code Style
```java
// Entities require strict JPA mapping attributes
@Entity
@Table(name = "tickers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(precision = 18, scale = 4)
    private BigDecimal price;
}

// Sequenced Collections over list.get(0)
var first = list.getFirst();
var last = list.getLast();

// Spring AOP Proxy Fallback pattern
@Autowired @Lazy private MyService self;
public void execute() {
    MyService proxy = (self != null) ? self : this;
    proxy.transactionalMethod();
}

// Explicit Timeframe Branching Pattern
if (timeframe == Timeframe.DAILY) {
    // daily logic
} else if (timeframe == Timeframe.WEEKLY) {
    // weekly logic
} else {
    log.error("Unsupported timeframe: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
}

// Descriptive Variable Naming Conventions
for (Bucket bucket : buckets) {
    // 'bucket' instead of 'b'
}
for (PriceBar bar : bars) {
    // 'bar' instead of 'b'
}
// Descriptive lambda parameters
dailyBars.stream().map(dailyBar -> ...);
matches.stream().map(match -> ...);

// Standard loop counters permitted for indexed loops
for (int i = 0; i < bars.size(); i++) {
    // 'i', 'j', 'k' permitted
}

// Actual Logic / Happy Path in 'if', Error/Warning/Insufficient Data in 'else'
if (dailyBars.size() >= MIN_BARS) {
    log.info("Ticker {}: Timeframe {} - Calculating...", ticker.getTickerSymbol(), Timeframe.DAILY);
    LocalDate lastDailyDate = getLastComputedDate(ticker, Timeframe.DAILY);
    dailyRecomputeStartDate = calculateRecomputeStartDate(dailyBars, lastDailyDate);
    dailyMatches = computePatterns(dailyBars, dailyRecomputeStartDate);
} else {
    log.debug(
        "Ticker {}: Insufficient daily data points (found {}) to compute patterns.",
        ticker.getTickerSymbol(),
        dailyBars.size());
}
```

## Testing
- **Framework**: JUnit 5 + JaCoCo
- **Coverage threshold**: 100% line/branch coverage for non-DTO packages.
- **Mocking**: Use `@MockBean` / Mockito. Ensure no live database connections in unit tests.
- **Determinism**: Use `HISTORY_WINDOW` to parameterize offset calculations and avoid N+1 queries.

## API Design & Naming
- **Enum URL Parameters**: Use full lowercase words (e.g., `?timeframe=daily`) for readability.
- **Enum String Lookup via Enum Method**: Add a static `from(String)` method directly in the enum to convert string parameters (case-insensitively) to the enum, rather than maintaining separate `Converter<String, EnumType>` classes.
- **Endpoint Clarity**: Keep static config endpoints (e.g., `/api/indicator-definitions`) distinct from computed data endpoints (e.g., `/api/tickers/{symbol}/indicators`).

## Backend Architecture & Package Structure
- **Shared Domain Concepts**: Enums and classes used across multiple boundaries (like `Timeframe`) must reside in a shared package (e.g., `com.alphaflow.common.enums`) rather than a specific layer like `persistence`.
- **Algorithm Configurability & Clean Architecture**: Avoid littering with `@Value` annotations inside the core engine classes; instead, use a config class and inject it.
- **JPA Property Paths in JPQL**: Always use exact Java entity field names (e.g. `iv.indicatorDefinition.indicatorDefinitionId`) in `@Query` JPQL definitions rather than DB column names to avoid `QueryCreationException` on context startup.
- **DTO Isolation from Schema Renames**: When refactoring entity primary keys to `<entity>_id` formats, maintain existing DTO property names (e.g. `ChartPatternDto.id`) to avoid breaking frontend REST clients.
- **Atomic Bulk JPQL Deletes for Retention Cleanup**: Prefer `@Modifying @Query("DELETE FROM ...")` single-statement JPQL deletes over Spring Data derived deletion methods (such as `deleteByTickerAndPriceDateLessThan(...)`). Derived delete methods issue individual $N+1$ select queries followed by entity-by-entity deletions, whereas `@Modifying @Query` executes an atomic $O(1)$ database operation.
- **Window Filtering & Typical Price Boundary Calculations**: When filtering zone midpoint levels within a dynamic percentage window around price action, derive Typical Price ($TP = \frac{\text{high} + \text{low} + \text{close}}{3}$) from the reference candle at scale 12 (`RoundingMode.HALF_UP`). Validate `rangePct` for boundary safety (`rangePct > 0` and finite/non-NaN) and provide an unwindowed fallback when the reference candle is not present.
- **No Defaults for Config Variables or Request Parameters**: Never provide default values for configuration properties (in `@ConfigurationProperties` classes) or request parameters (`@RequestParam(defaultValue = ...)`) unless specifically mentioned and requested. Configuration properties must be declared without field initializers in Java classes and sourced explicitly from external configuration (`application.properties` / environment variables).

## Technical Indicators & Mathematical Calculations
- **BigDecimal Math Precision & Stability**: When computing standard deviation or variance over sliding windows:
  - Protect standard deviation `.sqrt()` calculations with a scale-bounded `MathContext` (e.g., scale limit of `INTERNAL_SCALE + 4`) to ensure determinism and avoid decimal overflows.
  - Handle potential floating-point precision-based negative variance values by guarding them (e.g. check `variance.signum() < 0` and set to `BigDecimal.ZERO`).
  - Provide fallback calculations using unweighted Simple Moving Average and standard deviation when total volume or source values within a sliding window are zero to avoid division by zero.
- **Config Parameter Types Limit**: The `IndicatorParams` helper class parses and holds parameters inside a `Map<String, Integer>`, restricting indicator configuration inputs to integer types. To support fractional multipliers (like a standard deviation multiplier of 2.5), the parsing architecture would need significant refactoring; standard integer parameters (e.g. `2`) or defaults should be preferred.
- **O(1) Rolling Sum Calculations & Statistical Identity**: For rolling moving average and variance indicators (like Bollinger Bands), maintain rolling sums of elements, weighted values, and weighted squared values. This allows computing metrics in $O(1)$ time per bar by adding the new bar's terms and subtracting the old bar's terms as the window slides. Calculate variance using the statistical identity $Var(X) = E[X^2] - (E[X])^2$ to eliminate nested loop iterations.
- **Typical Price Calculation**: For indicators using representative price ranges, calculate Typical Price ($TP = \frac{\text{high} + \text{low} + \text{close}}{3}$) rather than Close prices, and perform all arithmetic at the internal scale of 12 decimal places.

## Build & Tooling
- **Stale Gradle Configuration Cache**: Spotless or other Gradle linting plugins might throw stale cache errors when local JVM parameters, toolchain configurations, or Gradle versions change. If a `Spotless JVM-local cache is stale` error is encountered, delete `.gradle/configuration-cache/` to resolve the cache corruption.
- **Java Toolchains Version Alignment**: Ensure the local system JDK aligns with the toolchain version configured in `build.gradle` (e.g., `JavaLanguageVersion.of(...)`). This avoids compiler/toolchain resolution errors during automated builds or static analysis.

## Business Rules (Multi-Market)
- US equities: `ticker_type = 'US-EQUITY'`, `currency = 'USD'`, `timezone = 'America/New_York'`.
- Indian equities: `ticker_type = 'IN-EQUITY'`, `currency = 'INR'`, `timezone = 'Asia/Kolkata'`. (Suffix `.NS` for NSE).
- Crypto: `ticker_type = 'CRYPTO'`, `currency = 'USD'`, `timezone = 'UTC'`.
- Commodities: `ticker_type = 'COMMODITY'`, `currency = 'USD'`, `timezone = 'America/New_York'`.

## Technical Analysis & S&R Engine
- **Decoupling False Breakout Forgiveness from Touch Scoring**: Forgiving a temporary breach (preventing premature invalidation) must not conflate with validating support/resistance strength. Reclaims should never award touch credits, and false breakouts must be capped per level lifecycle (`max-false-breakouts`) to prevent whipsawed chop ranges from persisting indefinitely.
- **Sequential Candle Boundary Anchoring**: In `computeBuckets`, the latest price candle (`bars.getLast()`) defines both the pivot anchor $P$ and linear bucket intervals $[Z_{\text{bottom}}, Z_{\text{top}}]$. Because the latest candle is also scanned during historical candle iteration, its price bounds evaluate against the computed zone boundaries.
- **Dynamic Bucket Safety & Memory Protection**: When deriving dynamic bucket counts ($k_{\text{up}}, k_{\text{down}}$) from historical price extremes ($\text{minPrice}, \text{maxPrice}$), penny stocks or wide split-adjusted historical ranges can generate thousands of granular intervals. Always cap bucket creation with a safety ceiling (`maxAllowedBuckets = 500`) configured via a dedicated configuration properties class (`SupportResistanceConfig`) to safeguard against heap exhaustion.
- **Touch Detection Branch Leanliness for 100% Branch Coverage**: Avoid mathematically redundant boundary checks in support and resistance touch/slice calculations (such as checking `bar.close() >= bucket.getBottom()` when `bar.low() >= bucket.getBottom()` is already guaranteed). Redundant boundary checks introduce unreachable branches in bytecode that prevent achieving 100% JaCoCo branch coverage.
- **Subroutine Validation Order**: In multi-step calculation flows (e.g. `saveSupportResistances`), perform caller-level `timeframe` validation before delegating to subroutines that also validate `timeframe` (e.g. `isAlreadyComputed`). Otherwise, the subroutine throws first, leaving the caller's terminal unsupported timeframe branch unreachable during testing.

## Market Calendars & Data Downloader Gating
- **Market Calendar & Downloader Gating Pattern**:
  - `DailyPriceRepository.findLatestPriceDatesForActiveTickers()` provides a single bulk JPQL query mapping `Ticker` to `LocalDate` (`1900-01-01` if no prices exist). Comparing `latestSavedDate >= expectedTradingDate` allows $O(1)$ memory checks per ticker, eliminating redundant HTTP calls and throttle sleeps.
  - Setting `endTs` to at least `expectedTradingDate.plusDays(1).atStartOfDay(marketZone).toEpochSecond()` (or `Instant.now()`) allows ingestion of finalized daily candles immediately after market cutoff, avoiding UTC start-of-day truncation limitations.
- **Downstream Pipeline Short-Circuiting**:
  - Downstream pipeline steps (weekly candles, support/resistance, candlestick patterns, chart patterns, indicators) should be conditionally bypassed when the daily ingestion step yields zero newly ingested rows across all tickers, logging the short-circuit duration cleanly.
- **JaCoCo Branch Coverage on Downloader Logic**:
  - Redundant guards in inner download methods (such as checking `startTs >= endTs` when caller gating already guarantees `latestSavedDate < expectedTradingDate`) introduce unreachable branches that violate the 100% JaCoCo branch coverage requirement. Placing gating cleanly at the caller loop level keeps inner methods branch-lean and fully coverable.
- **Startup In-Memory Cache for Static Reference Data**:
  - Preloading static multi-year holiday reference data from JPA repositories into an in-memory `Map<Country, Set<LocalDate>>` on application startup (`@PostConstruct`) prevents database contention in high-frequency downloader loops and allows static calendar helper methods to remain branch-lean with $O(1)$ memory lookups.

## Persistence Layer & Repository Hygiene
- **Repository Dead Method Cleanliness & Import Hygiene**: Regularly audit and prune dead Spring Data JPA derived query methods when services transition to specialized bulk queries or JPQL projections. When pruning unused repository methods, always remove now-unused imports (e.g., `java.util.List`, `java.util.Optional`, or unused Enums) to maintain strict Checkstyle and Spotless compliance.

## Notification Services & External HTTP Integration
- **Lightweight In-Memory Mocking for RestClient Testing**:
  - Prefer `com.sun.net.httpserver.HttpServer` with a wildcard root handler (`/`) for unit testing Spring Boot 3 `RestClient` components. This allows deterministic validation of query parameter URI encoding (`parse_mode`, `chat_id`, `text`), status code branches (200 OK, 4xx/5xx errors), and connection failures without live network calls, third-party wiremock servers, or booting the Spring ApplicationContext.
- **JaCoCo Branch Coverage in Enum Parsers & Dynamic URL Construction**:
  - In static `from(String)` enum parsers, avoid checking both `status.name().equals(normalized)` and `status.value.equalsIgnoreCase(normalized)` when `normalized` is already uppercased. Since `name()` and uppercase `value` are identical strings, the second condition is unreachable in bytecode and penalizes branch coverage; check `status.name().equals(normalized)` directly.
  - When dynamically augmenting query strings, branch on `if (resolved.contains("?"))` upfront rather than checking `if (!resolved.contains("?"))` sequentially across multiple parameter evaluations. Sequential checks create unreachable `else` branches in bytecode if an earlier evaluation already appended query parameters.
- **PMD Static Analysis & Code Smells**:
  - Explicitly annotate single-abstract-method service interfaces with `@FunctionalInterface` to comply with PMD's `ImplicitFunctionalInterface` rule.
  - Always provide an explicit character set (e.g. `StandardCharsets.UTF_8`) to `String.getBytes(...)` in tests and network handlers to conform to PMD's `RelianceOnDefaultCharset` check.
- **Sensitive Credential Sanitization in Error Payloads**:
  - When logging network errors or persisting failures to audit tables (e.g., `notification_logs`), always sanitize sensitive bot credentials or API tokens (e.g., replacing occurrences with `******`) before logging or DB persistence to prevent secret leakage in diagnostic logs.



