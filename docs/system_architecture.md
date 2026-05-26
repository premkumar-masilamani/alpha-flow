# Alpha Flow: Architectural Specification (Current vs. Proposed EOD Architecture)

This document details the system architecture of the **Alpha Flow** trading application. It provides an analysis of the current architecture inferred from the codebase, followed by the proposed structural changes optimized for End-of-Day (EOD) daily-scale trading decision-making and secure authentication.

---

## 1. Current Architecture (Inferred from Code)

The system currently operates as a standard 3-tier monolithic web application with external data ingestion.

### Current Architecture Diagram
The generated diagram is available at [architecture.svg](file:///Users/premkumar/Code/alpha-flow/docs/diagrams/architecture.svg) (source code: [architecture.d2](file:///Users/premkumar/Code/alpha-flow/docs/diagrams/architecture.d2)).

### Components

#### A. Database Layer (PostgreSQL)
* **Metadata Table**: `tickers` tracks the list of tradeable assets (e.g. symbol, name, status, category like `US-EQUITY` or `IN-EQUITY`).
* **Time-Series Tables**:
  - `candle_bars` stores historical daily OHLCV, Volume, VWAP, and volume-profile capital distributions (POC, VAH, VAL, buyer/total capital) keyed by `(ticker_id, candle_date)`.
  - `indicators` stores computed indicators (e.g. SMA, EMA) categorized by type, period, and date.
* **Backtesting Tables**: `backtest_equity`, `backtest_signal`, `backtest_trade`, and `backtest_cagr` persist simulated strategy performance.
* **Management**: Relational schema structure managed by `golang-migrate` SQL migrations.

#### B. Ingestion & Core Engine (Spring Boot / Java 21)
* **Scheduler (`CoreScheduler`)**: Runs automatically on application startup (`ApplicationReadyEvent`) and schedules hourly updates (`0 0 * * * *`).
* **Downloader (`YahooFinanceDownloader`)**: Fetches historical data from Yahoo Finance via REST. It calculates a timestamp range, makes the call, and parses JSON responses.
* **Computations (`IndicatorCalculator`)**: Calculates indicators sequentially in-memory and stores the results in the database.
* **Backtester (`CandlestickBacktester`)**: Backtests candlestick rules against historical database candles.
* **API Controller Layer**: Exposes public, unauthenticated REST controllers (`TickerController`, `CandleDataController`, etc.) returning JSON.

#### C. Frontend Dashboard (Vite / React)
* **Visuals**: Displays candlestick charts using `lightweight-charts` and tabular data (indicators, signals, decision metrics).
* **Communication**: Communicates with the backend on port `8080` via Axios client requests.
* **Security**: Currently completely open with no authentication or user authorization constraints.

---

## 2. Proposed EOD Daily Processing Architecture

Since your trading logic is based strictly on **daily data post-market close**, we can eliminate complex real-time elements (like Kafka pipelines or Redis active-trade caches) and design a streamlined, robust EOD Batch Processing pipeline.

### Proposed Architecture Diagram
The generated diagram is available at [proposed_architecture.svg](file:///Users/premkumar/Code/alpha-flow/docs/diagrams/proposed_architecture.svg) (source code: [proposed_architecture.d2](file:///Users/premkumar/Code/alpha-flow/docs/diagrams/proposed_architecture.d2)).

### A. EOD Processing Pipeline Flow

Instead of running hourly updates, the system transitions to a single structured daily EOD batch cycle.

### EOD Pipeline Diagram
The generated diagram is available at [eod_pipeline.svg](file:///Users/premkumar/Code/alpha-flow/docs/diagrams/eod_pipeline.svg) (source code: [eod_pipeline.d2](file:///Users/premkumar/Code/alpha-flow/docs/diagrams/eod_pipeline.d2)).

1. **Scheduled Trigger**: Core scheduler triggers once a day (e.g., at `0 0 19 * * MON-FRI` - 7:00 PM EST, after markets close and Yahoo Finance finalizes daily candles).
2. **Daily Ingestion**: `YahooFinanceDownloader` requests only the current day's candle bar. Our new idempotent filter checks the DB and drops duplicates.
3. **Indicator Calculation**: `IndicatorCalculator` computes the new daily SMA/EMA values for the newly ingested candle.
4. **Strategy Evaluation**: An **EOD Strategy Evaluator** runs active strategies on the latest market data to determine actions for the *next* trading day (e.g. BUY, SELL, HOLD, or EXIT).
5. **Trade Action Persistence**: If a decision changes (e.g., BUY signal), it is logged to a `trading_signals` table.
6. **Alert Dispatch**: Dispatches a push notification (via Firebase Cloud Messaging) or email to the user indicating: *"Next-day action generated: Buy AAPL at market open."*

---

## 3. Proposed Authentication Design (Firebase Auth Integration)

To secure the dashboard and segment users' portfolios, alerts, and custom strategies, we propose integrating **Firebase Authentication** on the frontend and securing the Spring Boot backend using a JWT validation filter.

### The Authentication Flow

```
[ Frontend Client ] -- Auth Credentials --> [ Firebase Auth Server ]
        ^                                               |
        |                                           ID Token (JWT)
        |                                               v
[ Frontend Client ] ----- Request + Bearer JWT ----> [ Spring Boot API ]
                                                        |
                                                (Firebase Admin SDK)
                                                        |
                                                        v
                                                [ Verify & Authorize ]
```

1. **Frontend Integration**:
   - Install `@firebase/app` and `@firebase/auth` on the React client.
   - Implement a beautiful dashboard login/signup screen.
   - Upon successful login, the Firebase client SDK provides a JSON Web Token (JWT) ID Token.
2. **API Communication**:
   - The frontend stores the ID Token in session memory and includes it in the `Authorization` header of all requests to the Spring Boot API:
     ```http
     Authorization: Bearer <FIREBASE_JWT_TOKEN>
     ```
3. **Backend Verification**:
   - Add the **Firebase Admin SDK** dependency to the backend.
   - Configure a Spring Security filter (`FirebaseTokenFilter`) that intercepts incoming API requests.
   - The filter extracts the Bearer token, validates it against Google's public certificates using `FirebaseAuth.getInstance().verifyIdToken()`, extracts the user's `uid`, and populates Spring's `SecurityContextHolder`.
4. **Database Multi-Tenancy**:
   - Add a `firebase_uid` (VARCHAR) column to tables that require user-specific partitioning:
     * `user_portfolios`
     * `backtest_results`
     * `active_alerts`
     * `user_strategies`
   - Use this `uid` in JPA query methods (e.g. `findByFirebaseUid(String uid)`) to ensure strict isolation of data.
