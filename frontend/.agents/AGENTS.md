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

### Ask first
- Modifying core chart plotting defaults or color schemes.

### Never do
- Hardcode indicator line/plot colors in the canvas drawing script.

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

## Chart Rendering & Indicators
- **Lightweight-Charts Multi-Line Overlays**: When plotting technical indicators with multiple line outputs (like Bollinger Bands) as overlays on the main price chart, returning multiple output definitions (e.g. `upper`, `middle`, `lower` from `outputsFor()`) automatically loops over them and draws multiple line series that align perfectly on the same scale.
- **Color Palettes & Styling**: Always configure consistent visual styling and lookup rules under `indicatorColors.ts` mapped by output key so line colors remain distinct, stable, and decoupled from the canvas drawing scripts. Use custom or standard palettes (e.g. Red for upper, Blue for middle, Green for lower) for Bollinger Bands to provide strong visual contrast.
