# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Technical Analysis is a trading strategy exploration system that combines Java Spring Boot backend services with Python prototyping for technical analysis algorithms. The system focuses on processing financial market data, generating trading signals, and performing technical analysis using various indicators like Renko charts and moving averages.

## Tech Stack

- **Backend**: Java 21 + Spring Boot 3.5.3, Gradle, PostgreSQL
- **Prototype/Analysis**: Python 3.13, Pandas, yfinance, matplotlib
- **Database**: PostgreSQL 16 with Docker
- **Build Tools**: Makefile, Gradle, Pipenv, just

## Development Commands

### Initial Setup
```bash
# Prerequisites: Java 21, Node 20, Python 3.13
make run_database    # Sets up PostgreSQL, runs migrations, imports seed data
make run_backend     # Starts Spring Boot with live reload
```

### Database Operations
```bash
make run_database           # Full database setup (network, container, migrations, seed data)
make connect_database       # Connect via psql CLI
make migrate_database DATABASE_MIGRATION_DIRECTION=up    # Run migrations up
make migrate_database DATABASE_MIGRATION_DIRECTION=down DATABASE_MIGRATION_STEP=1  # Roll back 1 migration
make clean                  # Remove containers, volumes, and network (with confirmation)
```

### Backend Development
```bash
make run_backend                    # Start with live reload and caching
./backend/gradlew -p ./backend build  # Build JAR
./backend/gradlew -p ./backend test    # Run tests
```

### Frontend Development (planned)
```bash
make run_frontend     # Start frontend development server
make check_frontend   # Run security audit, linting, and production build
```

### Python Prototype
```bash
cd prototype/
just setup           # Create virtual environment and install dependencies
just run             # Run both ASTA and Renko analysis
just asta            # Run main technical analysis pipeline
just renko           # Generate Renko charts for BTC-USD
pipenv shell         # Activate virtual environment
```

### Utilities
```bash
make diagrams        # Generate D2 diagrams from docs/diagrams/*.d2
```

## Architecture

### Multi-Component System
The project consists of three main components:
1. **Backend** (Java/Spring Boot): Production data processing service
2. **Prototype** (Python): Research and algorithm development
3. **Database** (PostgreSQL): Centralized data storage

### Backend Architecture (Java)
- **Main Application**: `TechnicalAnalysisApplication.java` - CommandLineRunner that orchestrates data services
- **Services Layer**:
  - `DataDownloadService` - Downloads market data from external sources
  - `DataProcessingService` - Processes raw data for technical analysis
- **Data Layer**:
  - **Entities**: `TradeData`, `Ticker`, `FileRecord` - JPA entities
  - **Repositories**: Spring Data JPA repositories for each entity
- **Configuration**: `AppConfig.java`, `Utils.java` for application setup

### Database Schema
Core tables:
- `tickers` - Financial instruments (BTC-USD, ETH-USD, etc.)
- `trade_data` - OHLCV data with technical indicators (composite key: ticker_id, trade_time)
- `files` - File processing tracking

### Python Prototype Architecture
- **Main Pipeline**: `main.py` - Orchestrates data download, Renko generation, and MA calculations
- **Data Processing**:
  - `candle_data.py` - Downloads and processes OHLCV data via yfinance
  - `renko_data.py` - Generates Renko chart data from candlestick data
  - `renko_ma.py` - Calculates moving averages on Renko data
- **Configuration**: `config/config.json` - Tickers, timeframes, MA periods, Renko settings

### Key Data Flow
1. **Data Acquisition**: yfinance → CSV files OR external APIs → PostgreSQL
2. **Processing**: Raw OHLCV → Technical indicators → Renko charts → Moving averages
3. **Storage**: Local CSV files (prototype) + PostgreSQL (production)

## Environment Configuration

Key environment variables in `.env`:
- `POSTGRES_*` - Database connection settings
- `DOCKER_NETWORK_NAME` - Docker network for services
- `NEXT_PUBLIC_API_URL` - API endpoint for frontend
- `CORS_ALLOWED_ORIGINS` - CORS configuration

## Development Workflow

### For Java Backend Development
1. Ensure database is running: `make run_database`
2. Start backend with live reload: `make run_backend`
3. Backend runs data pipeline on startup (download + processing)

### For Python Algorithm Development
1. `cd prototype/`
2. `just setup` (first time)
3. Modify algorithms in `src/`
4. `just run` to test changes
5. Configuration changes go in `config/config.json`

### Database Development
- Migration files in `database/migrations/`
- Follow naming: `XXX_description.up.sql` / `XXX_description.down.sql`
- Seed data in `database/data/seed_data.sql`

## Technical Analysis Components

### Supported Indicators
- **Renko Charts**: Brick-based charting with configurable period count
- **Moving Averages**: Both SMA and EMA with multiple periods
- **Advanced Metrics**: VWAP, buyer ratios, volatility measures, VPIN

### Data Sources
- Primary: Yahoo Finance (yfinance)
- Supported tickers: BTC-USD, ETH-USD (configurable)
- Timeframes: 1d, 1wk (with next-timeframe analysis)
