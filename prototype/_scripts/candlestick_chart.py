import sys
import logging
from typing import Optional

import pandas as pd
import finplot as fplt

from helpers import load_timeseries_data, parse_chart_args, get_ohlcv_file_path
from constants import ZONE_COLORS, CHART_DISPLAY_ROWS

logger = logging.getLogger(__name__)


def format_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """Format the dataframe by ensuring correct dtypes for financial data."""
    print("[INFO] Formatting dataframe...")
    # Convert to datetime and handle timezones properly
    df["date"] = pd.to_datetime(df["date"])
    if df["date"].dt.tz is not None:  # If timezone-aware
        df["date"] = df["date"].dt.tz_convert(None)  # Convert to timezone-naive

    # Convert datetime to numerical representation if needed
    # (finplot can handle datetime objects directly)
    numeric_cols = ["open", "close", "high", "low"]
    df[numeric_cols] = df[numeric_cols].astype(float)
    print("[INFO] Dataframe formatted successfully.")
    return df


def create_candlestick_plot(
    df: pd.DataFrame, zones_df: Optional[pd.DataFrame] = None
) -> None:
    """Create and display a candlestick chart with support/resistance zones."""
    print("[INFO] Creating candlestick plot...")
    ax = fplt.create_plot("Candlestick Chart", init_zoom_periods=CHART_DISPLAY_ROWS)

    # Ensure we're using the correct column names and types
    df = df.sort_values("date").reset_index(drop=True)
    plot_data = df.rename(columns={"date": "time"})[
        ["time", "open", "close", "high", "low"]
    ].copy()

    # Plot candlesticks
    fplt.candlestick_ochl(plot_data, ax=ax)

    # Plot zones if available
    if zones_df is not None and not zones_df.empty:
        # Ensure active_from is datetime
        zones_df["active_from"] = pd.to_datetime(zones_df["active_from"])

        for _, row in zones_df.iterrows():
            start_time = df[df["date"] >= row["active_from"]]["date"].min()
            if pd.isna(start_time):
                continue

            end_time = df["date"].iloc[-1]
            zone_color = ZONE_COLORS.get(row["type"].lower(), "#646464")

            # Draw rectangle edges as individual lines
            fplt.add_line(
                (start_time, row["zone_lower"]),
                (end_time, row["zone_lower"]),
                color=zone_color,
                ax=ax,
            )  # bottom
            fplt.add_line(
                (start_time, row["zone_upper"]),
                (end_time, row["zone_upper"]),
                color=zone_color,
                ax=ax,
            )  # top
            fplt.add_line(
                (start_time, row["zone_lower"]),
                (start_time, row["zone_upper"]),
                color=zone_color,
                ax=ax,
            )  # left
            fplt.add_line(
                (end_time, row["zone_lower"]),
                (end_time, row["zone_upper"]),
                color=zone_color,
                ax=ax,
            )  # right

        print(f"[INFO] Added {len(zones_df)} support/resistance zones.")

    fplt.show()
    print("[INFO] Candlestick plot displayed.")


def main() -> None:
    """Main entry point for the program."""
    args = parse_chart_args()
    data_dir = args.data_dir.lower()
    ticker = args.ticker.upper()
    timeframe = args.timeframe.lower()

    chart_data_path = get_ohlcv_file_path(data_dir, ticker, timeframe)

    print(f"[INFO] Loading timeseries data from: {chart_data_path}")
    try:
        df = load_timeseries_data(chart_data_path)
        if df.empty:
            print(f"[ERROR] No valid data in {chart_data_path}")
            sys.exit(1)

        formatted_df = format_dataframe(df)
        zones_data = pd.DataFrame()
        create_candlestick_plot(formatted_df, zones_data)
    except Exception as e:
        print(f"[ERROR] An error occurred: {str(e)}")
        sys.exit(1)


if __name__ == "__main__":
    main()
