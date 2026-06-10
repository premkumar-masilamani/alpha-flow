# AGENTS.md

Operational guidance for AI coding agents working on the AlphaFlow codebase.

## Setup & Running Commands

All entry points go through the root `Makefile`. Running these targets is the canonical workflow:

```bash
make run_database       # Start Postgres container + run migrations + seed data
make migrate_database   # Run SQL migrations only (up)
make connect_database   # Connect to psql inside the running DB container
make run_backend        # Start Spring Boot backend with continuous live reload
make run_frontend       # Install dependencies and start Vite frontend dev server
make run_all            # Concurrently run database, backend, and frontend
make clean              # Tear down DB container, volume, and network (asks confirmation)
```

## System Architecture

AlphaFlow studies buying/selling flows to make better trading decisions. It is a three-tier application:

- `backend/` — Spring Boot 3.5 / Java 21 / Gradle
- `frontend/` — React 19 / Vite / TypeScript / Tailwind (charts via `lightweight-charts`)
- `database/` — Postgres 16 / Docker / migrations via `migrate/migrate`

### Multi-Market Support

- US equities: `ticker_type = 'US-EQUITY'`, `currency = 'USD'`, `timezone = 'America/New_York'`.
- Indian equities: `ticker_type = 'IN-EQUITY'`, `currency = 'INR'`, `timezone = 'Asia/Kolkata'`. Yahoo Finance suffix `.NS` for NSE.
- Crypto: `ticker_type = 'CRYPTO'`, `currency = 'USD'`, `timezone = 'UTC'`.
- Commodities: `ticker_type = 'COMMODITY'`, `currency = 'USD'`, `timezone = 'America/New_York'`.

Data source is Yahoo Finance (free tier). Daily and weekly timeframes only.

### Package Structure

```
com.alphaflow
├── persistence/           # Database layer
│   ├── entities/          # JPA entities (Ticker, DailyPrice, WeeklyPrice, IndicatorDefinition, Indicator, DailyIndicator, WeeklyIndicator, AnalysisResult)
│   ├── repositories/      # Spring Data JPA repositories
│   ├── enums/             # IndicatorType, Timeframe, PriceSource
│   └── exceptions/        # ResourceNotFoundException
├── engine/                # Computation layer (no API dependencies)
│   ├── configs/           # @ConfigurationProperties (IndicatorConfig, YahooFinanceConfig)
│   ├── downloaders/       # YahooFinanceDownloader
│   ├── calculators/       # WeeklyPriceCalculator, IndicatorCalculator
│   │   └── indicators/    # Indicator interface + implementations (SMA, EMA, RSI, MACD, Stochastic)
│   └── schedulers/        # CoreScheduler
└── api/                   # REST API layer
    ├── configs/           # ApiProperties, CorsConfig
    ├── controllers/       # REST controllers
    ├── services/          # Service layer
    ├── mappers/           # DTO mappers
    ├── dtos/              # Data transfer objects
    └── handlers/          # GlobalExceptionHandler
```

---

## Project Boundaries

### Always Do
- Run migrations via SQL scripts in `database/migrations/`. JPA `ddl-auto=none` is enforced.
- Follow strict 1-indexed numbering for migration script pairs.

### Ask First
- Modifying scheduled core jobs or the downloader frequency/rate-limiting logic.

### Never Do
- Do not commit secrets, private API keys, or `.env` files.

---

## AI Learnings & Coding Guidelines

### Repository Default Methods
- For simple entity-fetching operations and DTO mappings that are called directly from controllers or other packages, consider writing them as `default` methods in Repository interfaces. This helps eliminate thin, redundant API service classes and decouples the API service package from the strategy engine.

### Java 21 Sequenced Collections
- Always prefer calling `.getFirst()` and `.getLast()` instead of `.get(0)` and `.get(list.size() - 1)` respectively for all `List` or sequenced collection retrievals to maintain code clarity and use modern Java API features.

### Range-Fetching and In-Memory Slicing
- For chronological historical analysis, replace loop-based database queries with a single range fetch. Iterate chronologically in memory and slice the history using `HISTORY_WINDOW` up to each target date to avoid look-ahead bias and N+1 database queries. Use `HISTORY_WINDOW` consistently to parameterize offset calculations in both production and test cases.

### Package Separation Boundaries & Layering
- Always enforce the strict layered hierarchy:
  - `api` can import/depend on `engine` and `persistence`.
  - `engine` can depend on `persistence` but must **never** reference classes/DTOs in the `api` package.
  - `persistence` must remain completely self-contained and **never** import classes from `engine` or `api`.
- Run validation checks via `make check_separation` to prevent package boundary erosion over time.
- **DTO Mapping Tradeoffs & Decoupling**: 
  - Placing DTO mapping default methods directly inside repositories couples the `persistence` package to `api` DTOs, creating a boundary violation. Perform DTO mapping exclusively in the `api` services layer (e.g., creating dedicated service classes like `DailyPriceService` and `AnalysisService` to map queries).
  - Business logic/strategy execution classes (e.g., `ASTAStrategy`) must not return or handle API DTOs. Strategy engines should expose database entity results directly and delegate DTO transformation to the `api` services layer.
- **Entity Self-Containment**: Database entities in the `persistence` layer must never import business logic utility classes (e.g., classes in the `engine` layer like `IndicatorParams`). Instead, serialize or format parameter configurations natively using basic Java standard constructs (such as `TreeMap` and `StringJoiner`) inside the entity methods.

### Visualizer Rendering (Cytoscape.js)
- When generating interactive canvas graphs (like `index.html` via Cytoscape), always specify raw hex colors instead of CSS variables (`var(...)`), as the canvas rendering context does not resolve CSS custom properties.
- Use hierarchical layouts (e.g. Dagre ranking top-to-bottom) for layered architectures instead of force-directed spring layouts (e.g. Cose), which result in unreadable node clusters and overlapping parent boundaries.

