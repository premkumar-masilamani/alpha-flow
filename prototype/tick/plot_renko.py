import argparse
import logging
import pandas as pd

from helpers import load_timeseries_data
from renko_chart import plot_renko, get_prices

logger = logging.getLogger(__name__)

CHART_BRICKS_COUNT: int = 180  # 6 Months Data

if __name__ == "__main__":
    """Main entry point for the program."""
    parser = argparse.ArgumentParser(
        description="Plot Renko Chart with VWAP."
    )
    parser.add_argument("--renko-file-path")
    args = parser.parse_args()

    renko_df = load_timeseries_data(args.renko_file_path.lower())

    if len(renko_df) > CHART_BRICKS_COUNT:
        renko_df = renko_df.iloc[-CHART_BRICKS_COUNT:]

    # Get current price and SL price
    current_price, sl_price = get_prices(renko_df)
    logger.info(f"Calculated Current Price: {current_price}, SL Price: {sl_price}")

    # Plot Renko Chart wth GMMA Indicator
    plot_renko(renko_df, pd.DataFrame(), "BTCUSDT", "1d")

# Example:
# python3 tick/plot_renko.py --renko-file-path ./data/BTCUSDT-2025-1d-renko-vwap.csv
