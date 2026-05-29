# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

AlphaFlow studies the flow of buying and selling to make better trading decisions. It is a three-tier app:

- `backend/` — Spring Boot 3.5 / Java 21 (Gradle)
- `frontend/` — React 19 + Vite + TypeScript + Tailwind, charts via `lightweight-charts`
- `database/` — Postgres 16 (Docker), schema managed by `migrate/migrate` SQL migrations, seed data (`database/data/seed_data.sql`) imported on startup

## Common commands

All entry points go through the root `Makefile`, which loads `.env` and exports every key in it. There is no separate dev-bootstrap script — running these targets is the canonical workflow.

```
make run_database       # network + Postgres container + migrate + seed (rebuilds the container each run)
make migrate_database   # runs SQL migrations only (up by default)
make connect_database   # psql into the running container
make run_backend        # gradle bootRun --continuous (live reload)
make run_frontend       # npm install + vite dev server
make run_all            # run_database, then backend + frontend concurrently
make check_frontend     # npm ci + audit + lint + production build (CI-style frontend check)
make clean              # destroys container + volume + network (double-prompt confirms)
```

Backend tests: `./backend/gradlew -p ./backend test` (JUnit 5). Single test: `./backend/gradlew -p ./backend test --tests "ClassName.methodName"`.

Frontend lint/build: `npm --prefix frontend run lint` / `npm --prefix frontend run build`.

Migrations are numbered SQL pairs (`NNN_name.up.sql` / `NNN_name.down.sql`) in `database/migrations/`. To roll back: `docker run --rm --network $DOCKER_NETWORK_NAME -v ./database/migrations:/migrations migrate/migrate:4 -path=/migrations -database "postgres://..." down 1`.

## Architecture

### Data pipeline (the core loop)

`engine/schedulers/CoreScheduler` is the heart of the backend. It fires:
- On `ApplicationReadyEvent` (every backend startup), and
- Hourly via `@Scheduled(cron = "0 0 * * * *")`.

Each run executes two steps sequentially:
1. `engine/downloaders/YahooFinanceDownloader` — pulls daily OHLCV from Yahoo's chart API for every active ticker, writes `daily_prices`.
2. `engine/calculators/WeeklyPriceCalculator` — rolls daily prices up into `weekly_prices` (per-ticker, grouped by week-starting Monday).

If you add a new ingestion source or a new aggregation, this is the pipeline to plug into. The schema is intentionally minimal: only `tickers`, `daily_prices`, and `weekly_prices` exist.

### Backend package layout (`com.alphaflow`)

- `api/` — HTTP-facing layer. `controllers/` (`TickerController`, `DailyPriceController`, and `ApiController` for the index/error routes) are thin and delegate to `services/` (`TickerService`, `DailyPriceService`). `dtos/` are the wire types (`TickerDTO`, `OhlcvDTO`); `mappers/` (`TickerMapper`, `OhlcvMapper`) convert entities → DTOs. `configs/CorsConfig` reads `alphaflow.cors.allowed-origins`; `filters/AuditLoggingFilter` logs requests.
- `engine/` — the data pipeline. `downloaders/YahooFinanceDownloader` (ingestion), `calculators/WeeklyPriceCalculator` (daily→weekly aggregation), `schedulers/CoreScheduler` (orchestration), `configs/YahooFinanceConfig` (download URL + inter-request delay).
- `persistence/` — persistence + cross-cutting. `entities/` are JPA `@Entity` classes (`Ticker`, `DailyPrice`, `WeeklyPrice`), `repositories/` are Spring Data interfaces, `exceptions/` holds `ResourceNotFoundException`.

Hibernate `ddl-auto=none` — the DB schema is owned by the migration files in `database/migrations/`, never by JPA. When you add or change an entity, write a matching migration. Likewise when you add a migration, update the entity.

The REST API mounts at `/api`: `GET /api/tickers` lists active tickers, `GET /api/tickers/{symbol}/data` returns that ticker's daily OHLCV series (daily only — there is no weekly endpoint). OpenAPI/Swagger UI is at `/swagger-ui/index.html` (springdoc); Spring Actuator endpoints are at `/actuator/*`.

### Frontend

Small SPA in `frontend/src/`:
- `App.tsx` owns the global state (selected ticker, daily price data, layout flags) and orchestrates fetches. Each ticker has two views: a "Technical Analysis" overview table and a single daily candlestick "Technical Chart".
- `components/` — `Header`, `Sidebar` (flat ticker list, no grouping), `Chart` (lightweight-charts wrapper — daily candles + volume).
- `services/api.ts` — Axios client (`getTickers`, `getCandleData`) with a simple in-memory cache. Base URL comes from `NEXT_PUBLIC_API_URL` (defined in root `.env`, despite the Next-style name — this is a Vite app).

### Configuration

All config flows through the root `.env`:
- DB connection (`POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`)
- Docker (`DOCKER_NETWORK_NAME`, `DOCKER_POSTGRES_VOLUME`, images)
- `CORS_ALLOWED_ORIGINS` (consumed by Spring), `NEXT_PUBLIC_API_URL` (consumed by Vite)

`backend/src/main/resources/application.properties` references these env vars via `${...}` — do not hardcode connection or CORS values there.

## Conventions

- Prices are `BigDecimal` (`numeric(18,4)`); volume is `Long` (`bigint`). Don't introduce `double` into the pipeline.
- Migrations are append-only and paired (`.up.sql` + `.down.sql`). The current `001`–`003` are the consolidated baseline (tables / constraints / indices); add new changes as `004+`, and don't renumber existing files.
- Lombok is on the classpath — entities use it for boilerplate.
