# AlphaFlow Indicators — Design

> Status: **implemented through the API**. Done: schema + entities (migration `004`), the hand-rolled
> indicator engine (`engine/calculators/indicators/`), pipeline wiring (`IndicatorProperties`,
> `IndicatorTickerProcessor`, `IndicatorCalculator`, `CoreScheduler` Step 3/3), and the REST API
> (discovery + bulk-by-timeframe series, configurable per-timeframe window, weekly OHLCV endpoint).
> Remaining phase — frontend — not yet built.
> **End-to-end validated** against the live dockerized Postgres (2026-05-29): migration `004` applied
> clean; a full pipeline run over 10 tickers / 74k daily + 15k weekly bars produced ~193k+ indicator
> values + 70 checkpoints with no errors. Verified in the DB: the 7 configured combos with correct
> plot counts (MACD→3, Stochastic→2, rest→1); RSI & Stochastic ∈ [0,100]; recursive indicators carry
> `jsonb` internals (string-encoded decimals) while windowed SMA/Stochastic carry none; every
> checkpoint's `last_price_date` strictly lags the latest bar (the never-checkpoint-latest rule);
> warm-up row-count deltas match period differences. API verified live: `/api/indicators` discovery,
> bulk-by-timeframe series (multi-plot grouping), weekly OHLCV, plus 400 on bad timeframe / 404 on
> unknown ticker. (Still no automated integration test — this was a manual run.)

AlphaFlow computes technical indicators (EMA, SMA, RSI, MACD, Stochastic, …) per ticker,
per timeframe, and persists them so the request path stays thin. This document records the
design decisions and the reasoning behind them.

## Strategy

- **Precompute & persist** indicators in the pipeline (not computed on-the-fly per request).
  Indicators are the next aggregation layer after the daily→weekly rollup. Fits the existing
  `CoreScheduler` "download → aggregate → calculate" philosophy and keeps reads cheap.
- **Incremental, resume-from-persisted-state** computation — *not* full recompute every run.
  At the target scale (~500 tickers × ~15 daily + ~2 weekly indicators) the dominant cost is
  **DB write volume**: a full recompute rewrites every indicator row for every ticker daily
  (tens of millions of rows); incremental writes only the new bar's values (tens of thousands/day).
- **Hand-rolled in `BigDecimal`.** ta4j was evaluated and rejected: it computes over an
  in-memory `BarSeries` and has **no API to serialize an EMA's running value to the DB and
  resume from that scalar** given only the next bar. True resume-from-persisted-state requires
  owning the recursive step, so we implement the indicators ourselves. This also honors the
  project's "no `double` in the pipeline" convention end-to-end.

## Timeframes

- **Daily + Weekly only.** Modeled as a `Timeframe` enum so `MONTHLY` is a cheap future add.
  No new price rollups are built now (monthly would need a `monthly_prices` table + calculator).

## Storage

Two new tables (migration `004_indicator_tables`). Schema is owned by the SQL migrations
(`ddl-auto=none`); entities mirror them.

### `indicator_values` — published plot values (what the API serves)

Generic long/tall table — one row per **plot** (TradingView's mental model):
SMA→1 (`value`), EMA→1, RSI→1, MACD→3 (`macd`/`signal`/`histogram`), Stochastic→2 (`k`/`d`).

| column | type | notes |
|---|---|---|
| `indicator_value_id` | bigint identity | PK |
| `ticker_id` | bigint | FK → tickers, ON DELETE CASCADE |
| `timeframe` | varchar(16) | `DAILY` \| `WEEKLY` |
| `indicator_type` | varchar(32) | `SMA`/`EMA`/`RSI`/`MACD`/`STOCHASTIC` |
| `source` | varchar(16) | input field: `CLOSE`/`VOLUME`/`OPEN`/`HIGH`/`LOW` |
| `params` | varchar(128) | canonical param string, e.g. `period=14`, `fast=12,signal=9,slow=26` |
| `output_name` | varchar(32) | the plot: `value`/`macd`/`signal`/`histogram`/`k`/`d` |
| `price_date` | date | bar date |
| `value` | numeric(18,4) | **NOT NULL** — no rows during warm-up |

Natural key (UNIQUE): `(ticker_id, timeframe, indicator_type, source, params, output_name, price_date)`.

### `indicator_state` — resume checkpoints (internal)

One checkpoint row per `(ticker_id, timeframe, indicator_type, source, params)`. **Every** combo
gets a row (uniform), even windowed indicators (SMA/Stochastic) that leave `internals` empty —
so cold-start detection and the contiguity check are one uniform mechanism.

| column | type | notes |
|---|---|---|
| `indicator_state_id` | bigint identity | PK |
| `ticker_id` | bigint | FK → tickers, ON DELETE CASCADE |
| `timeframe` | varchar(16) | |
| `indicator_type` | varchar(32) | |
| `source` | varchar(16) | |
| `params` | varchar(128) | |
| `last_price_date` | date | checkpoint marker — **always lags the latest published bar by ≥1 finalized bar** |
| `internals` | jsonb (nullable) | recursive running state; **string-encoded decimals** for bit-exact resume |

Natural key (UNIQUE): `(ticker_id, timeframe, indicator_type, source, params)`.

`internals` examples — EMA `{"ema":"123.4500"}`; MACD `{"fastEma":"…","slowEma":"…","signalEma":"…"}`;
Wilder RSI `{"avgGain":"…","avgLoss":"…","prevClose":"…"}`; windowed (SMA/Stochastic) `null`.

## Incremental engine rules

- **Never checkpoint the most recent bar.** `indicator_state.last_price_date` always lags the
  latest published bar. Each run: load state → recompute forward over bars newer than the
  checkpoint → **upsert** published values → advance the checkpoint only once a *newer* bar proves
  the prior one final. This is safe for both timeframes with one rule:
  - Daily bars are already immutable (the downloader stops at start-of-today and never overwrites).
  - The current **weekly** bar mutates all week (`WeeklyTickerProcessor` overwrites it each run),
    so it must never become the checkpoint until the week closes.
- **Cold start / self-heal.** No checkpoint → full-history backfill (load the *entire* price
  series so the recursive seed is exact). Checkpoint present → contiguity check (checkpoint date
  present, no unexpected gaps between it and the latest); on failure → fall back to full recompute
  for that combo. Plus a **manual force-recompute** escape hatch. This auto-heals: first run,
  newly added indicator, newly added ticker, and missed runs.
- **Warm-up.** No rows are stored until the indicator is fully defined. **Standard seeding**:
  EMA seeded from the SMA of the first N values; Wilder RSI seeded from the simple average of the
  first 14 deltas. Insufficient bars = normal no-op (not an error).

## Code shape (engine — implemented in `engine/calculators/indicators/`)

- **`Indicator` interface**, one bean per family (`EmaIndicator`, `RsiIndicator`, `MacdIndicator`,
  `StochasticIndicator`, `SmaIndicator`), discovered via `Map<String, Indicator>` keyed by `type()`.
  Contract: `compute(bars, priorState, params) → { values, newState }`. A timeframe-agnostic
  `PriceBar` view exposes all OHLCV fields; `source` selects the field for single-series indicators.
  Multi-field indicators (Stochastic) read what they intrinsically need.
- **Pipeline:** `IndicatorCalculator` (loops active tickers, isolates per-ticker failures, mirrors
  `WeeklyPriceCalculator`) + `IndicatorTickerProcessor` (`@Transactional` per ticker — published
  values + checkpoint committed atomically; separate bean to honor the Spring proxy boundary).
  Added as **Step 3/3** in `CoreScheduler.run()` after weekly, inside the existing `running` guard.
  (Ordering is guaranteed by the sequential pipeline: indicators read current `weekly_prices`.)
- **Tests:** per-indicator unit tests asserting **resume == full-backfill bit-for-bit**, plus
  known reference values.

## Configuration (global, hardcoded via `@ConfigurationProperties`)

The compute matrix is externalized as typed config (not DB-driven, not a Java enum), matching the
"config flows through `application.properties`/`.env`" convention. The indicator *math* stays in
code; only *which combos to run* is config.

v1 matrix (source `close` unless noted):

| Timeframe | Indicator | Source |
|---|---|---|
| Daily | EMA-5, EMA-13, EMA-26 | close |
| Daily | RSI-14 | close |
| Daily | Stochastic(14,3,3) — standard (range from price_high/price_low) | close |
| Daily | SMA-20 | **volume** |
| Weekly | MACD(12,26,9) — emits macd/signal/histogram | close |

## API (implemented in `api/`)

- `GET /api/indicators` — discovery; returns the configured matrix so the UI builds its controls
  from config rather than hardcoding the list.
- `GET /api/tickers/{symbol}/indicators?timeframe=DAILY` — bulk-by-timeframe series, grouped by indicator.
- Result window **mirrors the price endpoint and is configurable per-timeframe** (lift the
  hardcoded `180` out of `DailyPriceService` into typed config; default 180 daily).
- `GET /api/tickers/{symbol}/weekly-data` — trivial mirror of the daily OHLCV endpoint against
  `weekly_prices` (data ready for later weekly charting).

## Frontend (not yet built)

- **Daily chart overlays + oscillator sub-panes** (`lightweight-charts` line series + panes),
  toggles driven by the discovery endpoint — covers EMA/RSI/Stochastic/volume-SMA.
- **Weekly MACD shown numerically** (card/table) for v1; **full weekly charting deferred** as a
  fast follow (the weekly OHLCV endpoint is already in place).

## Deferred (explicitly out of scope for v1)

- Monthly (and intraday) timeframes.
- Per-ticker indicator configuration.
- Full weekly charting UI.
- Tail-only loading micro-optimization (cold start still reads full price history).
