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
- Apply database migrations (all migrations are strictly manual—never ever apply them).

## Project Structure
```text
backend/           # Spring Boot 3.5 / Java 26 / Gradle
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

## Engineering Learnings

### Build & Quality Tooling
- **Spotless Stale Configuration Cache**: If `make lint` fails with `Spotless JVM-local cache is stale` during `spotlessJava` when reusing the Gradle configuration cache, delete `.gradle/configuration-cache` (e.g. `rm -rf .gradle/configuration-cache backend/.gradle/configuration-cache`) to reset the stale cache.

### Technical Analysis & S&R Engine
- **Decoupling False Breakout Forgiveness from Touch Scoring**: Forgiving a temporary breach (preventing premature invalidation) must not conflate with validating support/resistance strength. Reclaims should never award touch credits, and false breakouts must be capped per level lifecycle (`max-false-breakouts`) to prevent whipsawed chop ranges from persisting indefinitely.
- **Sequential Candle Boundary Anchoring**: In `computeBuckets`, the latest price candle (`bars.getLast()`) defines both the pivot anchor $P$ and linear bucket intervals $[Z_{\text{bottom}}, Z_{\text{top}}]$. Because the latest candle is also scanned during historical candle iteration, its price bounds evaluate against the computed zone boundaries.
