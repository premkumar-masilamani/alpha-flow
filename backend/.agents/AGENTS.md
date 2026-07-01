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

// Java 21 Sequenced Collections over list.get(0)
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

## Indicator & Algorithmic Development
- **Lookahead Bias vs Break Checks**: When calculating trailing technicals (like Pivots), remember that the most recent `window` bars cannot form pivots. You must write secondary loops to check line crossings (breaks) against these final bars, otherwise recent market events are lost.
- **Polarity (Role Reversal)**: Avoid tracking the historical state of Support/Resistance polarity during runtime calculations. Track crossings (`breakCount`), cull heavily chopped lines, and assign the final polarity (Support or Resistance) statically at the very end based strictly on the expected line price vs current close price.
- **Historical Analysis limits**: When parsing all-time historical S&R data, use dynamic distance filters (like a 20% circuit breaker from the current price) to cull irrelevant historical levels and prevent database bloat, rather than arbitrary time-based cutoffs.
- **Mocking BigDecimals**: Always use `new BigDecimal("value")` instead of `valueOf` or long literals when mocking Entity accessors (like `getVolume()`) in Mockito to prevent class cast errors.
