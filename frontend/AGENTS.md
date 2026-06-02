# AGENTS.md - Frontend

Operational guidance for AI coding agents working on the React/TypeScript frontend (`frontend/` subdirectory).

## Setup & Running Commands

Run all commands from the `frontend/` directory:

```bash
npm install          # Install dependencies
npm run dev          # Start the local Vite development server
npm run build        # Build the production bundle (also compiles TypeScript)
npm run lint         # Run ESLint validation
npm run preview      # Locally preview the production build
```

## Frontend Architecture

The frontend is a lightweight Single Page App (SPA) structured under `frontend/src/`:

- `components/` — UI components like `Header`, `Sidebar` (ticker list), `Chart` (lightweight-charts candlestick & volume chart), `IndicatorControls` (controls for toggling indicators).
- `services/` — API clients. `api.ts` houses custom API calls using Axios (`getTickers`, `getCandleData`) with an in-memory cache to prevent redundant requests.
- `App.tsx` — Global state management (selected ticker, indicator toggles, loaded data series) and page layout.

## Coding Conventions & Style

### React & TypeScript
- Enforce TypeScript strict mode. Ensure all props and state variables are fully typed.
- Component style: use modern functional React components with hooks.
- For CSS and layout, use Tailwind CSS. Make components clean and visually premium.
- Charting: Use `lightweight-charts` API strictly for daily candlesticks, volume bars, and overlaying indicators.

### API & Data Fetching
- Consume the local API base URL via `NEXT_PUBLIC_API_URL` (configured in root `.env`).
- Utilize the in-memory cache configured in `services/api.ts` to cache ticker list and candle histories, minimizing unnecessary HTTP calls.

## Formatting & Linting Instructions

### Linting Checks
- **Command**: Run `npm run lint` in the `frontend/` directory.
- **Rules**: Zero ESLint warnings or errors are allowed. Always resolve unused imports, variables, and type warnings.

### Code Formatting
- **Indentation**: 
  - Use **4 spaces** for TypeScript (`.ts`) and React/TSX (`.tsx`) source files.
  - Use **2 spaces** for configuration files (`package.json`, `.json`, `eslint.config.js`, etc.).
- **Quotes**: Use single quotes (`'`) for string literals in TS/TSX.
- **Semicolons**: Always end statements with semicolons in TS/TSX files.
- **Import Ordering**: Group imports logically: React core/hooks first, external libraries (e.g. `axios`, `lightweight-charts`), internal components/services, assets/CSS.

### TypeScript Compilation & Build Verification
- **Command**: Run `npm run build` from the `frontend/` directory.
- This command triggers `tsc -b` (TypeScript Project Reference Compilation) and then builds the production code using Vite.
- Ensure that the TypeScript compiler passes with absolutely zero errors before any commit or PR submission.

