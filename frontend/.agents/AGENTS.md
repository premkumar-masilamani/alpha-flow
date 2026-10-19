# AGENTS.md - Frontend

## Commands
```bash
# Install dependencies
make install

# Start local Vite development server
make dev

# Create production build
make build

# Run ESLint validation
make lint

# Run audit, lint, and build checks
make check
```

## Boundaries

### Always do
- Enforce TypeScript strict mode.
- Use `lightweight-charts` API strictly for daily candlesticks, volume bars, and overlaying indicators.
- Specify raw hex colors instead of CSS variables (`var(...)`) for Cytoscape.js visualizer rendering.
- Explicitly fail fast or render informative error UI when required environment variables (e.g. API endpoints) or parameters are missing.

### Ask first
- Modifying core chart plotting defaults or color schemes.

### Never do
- Hardcode indicator line/plot colors in the canvas drawing script.
- Default any environment variable, configuration property, state, or API request parameter unless explicitly specified. Silent fallbacks (e.g. empty string fallbacks for API URLs) are strictly forbidden.

## Project Structure
```text
src/config/        # Configuration (indicator colors, theme)
src/components/    # React UI components (Chart, Sidebar, Header)
src/services/      # API clients (axios with in-memory caching)
src/App.tsx        # Global state and page layout
```

## Code Style
```tsx
// 4-spaces indentation for TS/TSX; use single quotes; always use semicolons
import React, { useState } from 'react';
import { Chart } from './components/Chart';

export const ExampleComponent = () => {
    const [active, setActive] = useState<boolean>(false);
    return <Chart active={active} />;
};
```
```json
// 2-spaces indentation for configuration files
{
  "name": "frontend"
}
```

## Testing
- **Framework**: Vitest / React Testing Library.
- **Conventions**: Ensure chart components properly mock canvas APIs if they use `lightweight-charts`.
- **Universal LocalStorage Mocking**: In Node 22+ test environments where global `localStorage` is partially defined but uninitialized, tests running in Node context throw `TypeError` on storage operations. Always define standard mock implementations on both `window.localStorage` and `globalThis.localStorage` in `setupTests.ts`.

## Chart Rendering & Indicators
- **Lightweight-Charts Multi-Line Overlays**: When plotting technical indicators with multiple line outputs (like Bollinger Bands) as overlays on the main price chart, returning multiple output definitions (e.g. `upper`, `middle`, `lower` from `outputsFor()`) automatically loops over them and draws multiple line series that align perfectly on the same scale.
- **Color Palettes & Styling**: Always configure consistent visual styling and lookup rules under `indicatorColors.ts` mapped by output key so line colors remain distinct, stable, and decoupled from the canvas drawing scripts. Use custom or standard palettes (e.g. Red for upper, Blue for middle, Green for lower) for Bollinger Bands to provide strong visual contrast.

## Patterns & Encyclopedia Architecture
- **Dual Encyclopedia Conventions**: Maintain UX and architectural parity between Candlestick Patterns and classical Chart Patterns (`config/*Patterns.ts`, `components/*PatternEncyclopedia.tsx`, and `components/*PatternModal.tsx`). Both feature live keyword search, category filter tabs with dynamic count badges, pure 150x130 SVG vector schematics, and deep-linking/smooth-scroll highlighting via `initialPatternId`.
- **Target & Stop Loss Level Indicators**: In technical analysis overview cards, always render tentative target and stop loss price levels accompanied by percentage distances calculated against the latest candle close (`pctChange(...)`) to provide immediate, actionable risk-reward context.
- **Backward-Compatible Pattern Dictionaries**: When expanding pattern configuration definitions, derive legacy lookup dictionaries (such as `CHART_PATTERNS_CONFIG`) dynamically via `Array.reduce` over the master pattern array (`CHART_PATTERNS`) to prevent duplicate data definitions while preserving canvas drawing and test contracts.

## Client-Side Caching & Header Layouts
- **Persistent LocalStorage Caching with TTL**: When fetching rarely changing configuration data (like market holiday calendars), persist payloads in `localStorage` accompanied by an explicit `expiresAt` timestamp (e.g. 24 hours). Re-fetch only when the cache is absent, corrupt, or expired to avoid unnecessary startup API latency.
- **Full Text Display for Descriptive Status Badges**: For informative badges displaying arbitrary localized domain descriptions (such as festival names), avoid using `truncate` or `overflow-hidden`. Allow responsive wrapping with `flex-wrap` and glowing indicator lights (`bg-emerald-400` / `bg-rose-400`) to communicate both visual and textual state cleanly.

