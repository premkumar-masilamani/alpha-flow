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

### Ask first
- Adding third-party libraries/dependencies to `build.gradle`.

### Never do
- Bypass `BigDecimal` for price data types (never use `double` or `float`).
- Use JPA `ddl-auto` to generate schemas.
- Omit `@Column(length = N)` on Strings or `precision` / `scale` on BigDecimals.

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
