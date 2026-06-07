# AGENTS.md - Frontend

Operational guidance for AI coding agents working on the React/TypeScript frontend (`frontend/` subdirectory).

## Setup & Running Commands

Run all operations using the module `Makefile` inside the `frontend/` directory:

```bash
make install         # Install dependencies
make dev             # Start the local Vite development server
make build           # Create production build
make preview         # Locally preview the production build
make lint            # Run ESLint validation
make audit           # Run npm security audit
make check           # Run audit, lint, and build checks
make clean           # Remove node_modules and dist
```

## Frontend Architecture

The frontend is a lightweight Single Page App (SPA) structured under `frontend/src/`:

- `config/` — Configuration files like `indicatorColors.ts` containing custom indicator mapping colors.
- `components/` — UI components like `Header`, `Sidebar` (ticker list), `Chart` (lightweight-charts candlestick & volume chart), `IndicatorControls` (controls for toggling indicators).
- `services/` — API clients. `api.ts` houses custom API calls using Axios (`getTickers`, `getCandleData`) with an in-memory cache to prevent redundant requests.
- `App.tsx` — Global state management (selected ticker, indicator toggles, loaded data series) and page layout.

## Coding Conventions & Style

### React & TypeScript
- Enforce TypeScript strict mode. Ensure all props and state variables are fully typed.
- Component style: use modern functional React components with hooks.
- For CSS and layout, use Tailwind CSS. Make components clean and visually premium.
- Charting: Use `lightweight-charts` API strictly for daily candlesticks, volume bars, and overlaying indicators.

### Indicator Theme & Colors Configuration
- **Centralized Colors Config**: All indicator line/plot colors must be declared inside [indicatorColors.ts](file:///Users/premkumar/Code/alpha-flow/frontend/src/config/indicatorColors.ts) rather than hardcoded in the canvas drawing script.
- **RSI Shading**: Styled with purple (`rgba(168, 85, 247, 0.1)`) at 10% opacity.
- **Stochastic Shading**: Styled with blue (`rgba(59, 130, 246, 0.1)`) at 10% opacity.
- **MACD Histogram**: Dynamic coloring (green `rgba(34, 197, 94, 0.7)` for positive values $\ge 0$, red `rgba(239, 68, 68, 0.7)` for negative values $< 0$).

### API & Data Fetching
- Consume the local API base URL via `NEXT_PUBLIC_API_URL` (configured in root `.env`).
- Utilize the in-memory cache configured in `services/api.ts` to cache ticker list and candle histories, minimizing unnecessary HTTP calls.

## Chart Viewport & Customizations

- **Chart Window Size**: The default viewport size is fixed at `CHART_WINDOW = 250` bars in the frontend (`services/api.ts`).
- **Right Margin Spacing**: The chart reserves exactly 10 empty bars of space at the extreme right to improve visibility of the latest candles and indicator lines.
- **Viewport Persistence**: The visible viewport is persisted and updated using logical index coordinates (`setVisibleLogicalRange`) instead of date timestamps (`setVisibleRange`). Logical coordinates prevent the chart from snapping back to the rightmost edge and discarding the 10-bar offset during re-renders, and are corrected for prepended candles when loading older data to avoid visual jumps.
- **Indicator Default Selection**: On daily charts, the `Vol (20)` (Volume SMA 20) indicator is checked/enabled by default alongside the standard EMA indicators.
- **Indicator Legends**: The main chart legend must always sort indicators in a fixed, predefined order: `EMA (5)`, `EMA (13)`, `EMA (26)`, `Vol (20)`. Any other custom indicators are appended at the end.

## Project Boundaries

### Ask First
- Modifying core chart plotting defaults or color schemes.

### Never Do
- Do not commit secrets, private API keys, or `.env` files.

## Formatting & Linting Instructions

### Linting Checks
- **Command**: Run `make lint` in the `frontend/` directory.
- **Rules**: Zero ESLint warnings or errors are allowed. Always resolve unused imports, variables, and type warnings.

### Code Formatting
- **Indentation**: 
  - Use **4 spaces** for TypeScript (`.ts`) and React/TSX (`.tsx`) source files.
  - Use **2 spaces** for configuration files (`package.json`, `.json`, `eslint.config.js`, etc.).
- **Quotes**: Use single quotes (`'`) for string literals in TS/TSX.
- **Semicolons**: Always end statements with semicolons in TS/TSX files.
- **Import Ordering**: Group imports logically: React core/hooks first, external libraries (e.g. `axios`, `lightweight-charts`), internal components/services, assets/CSS.

### TypeScript Compilation & Build Verification
- **Command**: Run `make build` from the `frontend/` directory.
- This command triggers `tsc -b` (TypeScript Project Reference Compilation) and then builds the production code using Vite. You can also run `make check` to run all validation checks (audit, lint, and build).
- Ensure that the TypeScript compiler passes with absolutely zero errors before any commit or PR submission.
