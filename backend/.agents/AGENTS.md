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
- Enforce strict boundaries: `api` depends on `engine` and `persistence`; `engine` depends on `persistence`; `persistence` is completely self-contained.
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

## Project Structure
```text
src/main/java/com/alphaflow/api/         # REST API layer (controllers, services, DTOs)
src/main/java/com/alphaflow/engine/      # Computation (downloaders, calculators, schedulers)
src/main/java/com/alphaflow/persistence/ # DB layer (JPA entities, repositories, enums)
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
    dailyMatches = computePatterns(dailyBars, lastDailyDate);
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
- **Spring Boot Converters**: Register custom `Converter<String, EnumType>` (like `TimeframeConverter`) to cleanly map parameters to uppercase Java Enums.
- **Endpoint Clarity**: Keep static config endpoints (e.g., `/api/indicator-definitions`) distinct from computed data endpoints (e.g., `/api/tickers/{symbol}/indicators`).

## Backend Architecture & Package Structure
- **Shared Domain Concepts**: Enums and classes used across multiple boundaries (like `Timeframe`) must reside in a shared package (e.g., `com.alphaflow.common.enums`) rather than a specific layer like `persistence`.
- **Algorithm Configurability & Clean Architecture**: Avoid littering with `@Value` annotations inside the core engine classes; instead, use a config class and inject it.

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
