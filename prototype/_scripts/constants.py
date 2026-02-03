from pathlib import Path

# === Core Parameters ===
SYMBOL = "BTC-USD"
TIMEFRAME = "1D"
START_DATE = "1900-01-01"
INVESTMENT = 100_000

# === Support/Resistance Parameters ===
PIVOT_LOOKBACK = 5  # Look for stronger pivot structures over a longer window
ZONE_TOLERANCE = 0.02  # 2% tolerance allows for slightly broader zones
TOUCHPOINT_DISTANCE = 5  # Ensures touches are meaningfully spaced
MIN_TOUCHES = 3  # Minimum touches to validate zone
RECENT_BARS = 90  # Lookback to check for broken zones
MAX_DEVIATION_FROM_PRICE = 0.2  # 20% deviation from current price
ZONE_COLORS = {
    "support": "#00FF00AA",  # Bold green with transparency
    "resistance": "#FF0000AA",  # Bold red with transparency
    "flip": "#800080AA",  # Bold purple with transparency
    "default": "#AAAAAA66",  # Gray fallback
}

# === Indicator Parameters ===
# MACD
MACD_FAST = 12
MACD_SLOW = 26
MACD_SIGNAL = 9

# === MA Parameters ===
COLUMN_NAME_MA = "ma"
MA_TYPES = ["sma", "ema", "wma", "hma", "alma"]
MA_START = 2
MA_END = 370

# === File Paths ===
DATA_DIR = Path("data")
DATA_DIR.mkdir(parents=True, exist_ok=True)

# === Chart Parameters ===
CHART_DISPLAY_ROWS = 180

OHLCV_FILE = DATA_DIR / f"{SYMBOL}_{TIMEFRAME}.csv"
MA_FILE = DATA_DIR / f"{SYMBOL}_{TIMEFRAME}_{COLUMN_NAME_MA}.csv"
MA_RESULTS_FILE = DATA_DIR / f"{SYMBOL}_{TIMEFRAME}_{COLUMN_NAME_MA}_results.csv"
