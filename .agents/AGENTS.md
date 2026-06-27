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

## Guidelines

Before working on a specific layer of the application (backend, frontend, or database), you must read the corresponding `.agents/AGENTS.md` file inside that specific submodule. It contains critical instructions and structural guidelines for that module.

---

## Project Boundaries

### Ask First
- Modifying scheduled core jobs or the downloader frequency/rate-limiting logic.

### Never Do
- Do not commit secrets, private API keys, or `.env` files.


