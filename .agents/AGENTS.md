# AGENTS.md

## Commands
```bash
# Start Postgres container + run migrations + seed data
make run_database

# Connect to psql inside the running DB container
make connect_database

# Start Spring Boot backend with continuous live reload
make run_backend

# Install dependencies and start Vite frontend dev server
make run_frontend

# Run tests and static analysis
make test
make lint
```

## Boundaries

### Always do
- Read the corresponding `.agents/AGENTS.md` file inside the `backend/`, `frontend/`, or `database/` submodules before modifying their files.

### Ask first
- Modifying scheduled core jobs or the downloader frequency/rate-limiting logic.

### Never do
- Commit secrets, private API keys, or `.env` files.
- Use DB migrations for DML operations. Migrations are strictly for DDL (schema changes).

## Project Structure
```text
backend/           # Spring Boot 3.5 / Java 21 / Gradle
frontend/          # React 19 / Vite / TypeScript / Tailwind
database/          # Postgres 16 / Docker / migrate
.agents/           # Agent rules and context
architecture/      # Visualizer scripts and D2 diagrams
```

## Git Workflow
- **Branch naming**: `feature/[short-desc]` or `fix/[short-desc]`.
- **Commit format**: Imperative mood (e.g. `Add MACD indicator`).
- **PR conventions**: One logical change per PR. Never push directly to main.

## Data Sections

### Business Rules (Multi-Market)
- US equities: `ticker_type = 'US-EQUITY'`, `currency = 'USD'`, `timezone = 'America/New_York'`.
- Indian equities: `ticker_type = 'IN-EQUITY'`, `currency = 'INR'`, `timezone = 'Asia/Kolkata'`. (Suffix `.NS` for NSE).
- Crypto: `ticker_type = 'CRYPTO'`, `currency = 'USD'`, `timezone = 'UTC'`.
- Commodities: `ticker_type = 'COMMODITY'`, `currency = 'USD'`, `timezone = 'America/New_York'`.
