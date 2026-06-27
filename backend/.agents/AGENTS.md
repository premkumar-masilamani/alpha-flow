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
