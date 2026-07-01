# Support & Resistance Line Generator

This document details the mathematical formulas, constraints, and algorithmic flow for generating historical and active support and resistance (S&R) lines in the AlphaFlow backend.

## 1. Overview and Core Engine Loop

The `SupportResistanceCalculator` computes S&R lines for each ticker using a lookback/lookahead `window` logic (defaulting to 10 for daily bars, 5 for weekly bars). The analysis is restricted to a rolling 2-year window from the latest price date.

The core algorithm operates as follows:
1. Identify all `Pivot High` and `Pivot Low` points.
2. Form **Horizontal Lines** by matching new pivots with past pivots within a 1% price tolerance.
3. Form **Angular (Trend) Lines** via linear regression on the most recent 4 pivots.
4. Scan forward through subsequent price action to detect line **Breaks**.
5. Apply multiple rounds of **Filtering** (e.g. minimum touches, proximity, circuit breakers).
6. Save valid lines to the database with explicit **Polarity** relative to the current closing price.

---

## 2. Pivot Detection

A pivot is a local extrema defined strictly by a surrounding window. 
- **Pivot High**: The bar's high price is strictly greater than or equal to all highs in the window `[i - window, i + window]`.
- **Pivot Low**: The bar's low price is strictly less than or equal to all lows in the window `[i - window, i + window]`.

*Note: Since the window looks ahead, pivot detection always stops at `bars.size() - window - 1`. The most recent `window` bars can never form a new pivot because they lack sufficient future context.*

---

## 3. Line Formation

### 3.1. Horizontal Lines
Horizontal lines represent price zones where the market has repeatedly bounced.

- **Tolerance**: A `1%` tolerance is used to match new pivots to past pivots or existing lines.
- **Seeding**: When a new pivot matches a past unused pivot (within 1% difference), a new horizontal line is initialized. The line's price level (`intercept`) is set to the arithmetic average of the two pivot prices:
  $$\text{Level} = \frac{\text{Pivot}_1 + \text{Pivot}_2}{2}$$
- **Expansion**: When subsequent pivots fall within 1% of the established line level, they are added as touchpoints.
- **Locking**: The line's established level is *never* recalculated. It remains locked to the average of the first two seeding pivots to prevent drifting.

### 3.2. Angular Trendlines
Angular lines represent diagonal support/resistance channels.

- **Trigger**: Every new pivot triggers a linear regression calculation on the last 4 pivots of the same type.
- **Math**: Simple linear regression ($y = mx + b$) where $x$ is the bar index and $y$ is the pivot price.
  $$m = \frac{\sum (x_i - \bar{x})(y_i - \bar{y})}{\sum (x_i - \bar{x})^2}$$
  $$b = \bar{y} - m\bar{x}$$
- **Formatting**: Slope and intercept are scaled to 4 decimal places (`RoundingMode.HALF_UP`).

---

## 4. Break Detection & Polarity Flipping

To avoid lookahead bias, a line is only subjected to break checks against a bar if the line was fully formed *before* that bar (i.e., all of its touchpoints are strictly in the past relative to the bar's date).

### 4.1. Expected Price Calculation
At any given index $i$, a line's expected price level is:
$$\text{Expected} = \text{Slope} \times i + \text{Intercept}$$

### 4.2. Break Rules
- A **Resistance** line is broken if the bar's `Close > Expected`.
- A **Support** line is broken if the bar's `Close < Expected`.

### 4.3. Polarity Flipping
When a line is broken:
- The `breakCount` is incremented.
- If `breakCount <= 2`, the polarity flips (Support becomes Resistance, or vice versa).
- If `breakCount > 2`, the line is permanently deactivated with `filterReason = BREAKS_GT_2`.

---

## 5. Filtering and Culling

After constructing all lines, the system applies rigorous filtering to reduce noise and database bloat. 

1. **Touch Minimums**: Horizontal lines must have $\ge 3$ touchpoints. Angular lines must have $\ge 4$ touchpoints. (Flags: `TOUCHES_LT_3`, `TOUCHES_LT_4`)
2. **Circuit Breaker**: A line whose *current* expected price is more than 20% above or below the current market close price is filtered. (Flag: `CIRCUIT_BREAKER_20_PCT`)
3. **Proximity Culling**: Lines are sorted descending by `importance` (number of touchpoints). If a weaker line's expected price is within 1% of a stronger line's expected price *at the current index*, the weaker line is absorbed/culled. (Flag: `PROXIMITY_1_PCT`)

---

## 6. Final Polarity Reset

To ensure UI plotting logic is absolutely foolproof, every valid line undergoes a final polarity reset relative to the *last* known closing price:
- If `CurrentExpected > CurrentClose` $\implies$ **RESISTANCE**
- If `CurrentExpected <= CurrentClose` $\implies$ **SUPPORT**

This guarantees that a line sitting below the current price will always be rendered green (Support) and a line above will always be red (Resistance), regardless of its historical flips.
