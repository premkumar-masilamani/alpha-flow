#!/usr/bin/env python3
"""
Daily to Renko Converter

This program reads daily OHLCV data from a CSV file and converts it to Renko chart data
using the VWAP (Volume Weighted Average Price) instead of close price.
The buyer_capital_ratio is preserved from input to output:
- If one day generates multiple Renko bricks, all bricks get the same buyer_capital_ratio
- If one Renko brick spans multiple days, it gets the average buyer_capital_ratio of those days

The output is written to a CSV file.
"""

import argparse
import pandas as pd
import sys
from pathlib import Path
from typing import List, Dict, Any
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


def get_zone_from_trend(trend: int) -> int:
    """Convert trend value to zone classification."""
    if trend <= 3:
        return 0
    elif trend <= 9:
        return 1
    elif trend <= 27:
        return 2
    elif trend <= 81:
        return 3
    else:
        return 4


def calculate_brick_size(df: pd.DataFrame, period_count: int) -> float:
    """
    Calculate the brick size for Renko chart based on the last N periods.

    Args:
        df: DataFrame containing OHLCV data with 'high' and 'low' columns
        period_count: Number of recent periods to use for calculation

    Returns:
        float: Calculated brick size (half of average range)
    """
    lookback_start = max(0, len(df) - period_count)
    lookback_df = df.iloc[lookback_start:]

    print("[INFO] Using the following periods for brick size calculation:")
    total_range = 0.0
    for idx, row in lookback_df.iterrows():
        high = row["high"]
        low = row["low"]
        rng = high - low
        total_range += rng
        # Assuming date column exists, if not will use index
        date_info = row.get("date", f"Index-{idx}")
        if hasattr(date_info, 'date'):
            date_str = date_info.date()
        else:
            date_str = str(date_info)
        print(
            f"  {date_str}: High = {high:.2f}, Low = {low:.2f}, Range = {rng:.2f}"
        )

    avg_range = total_range / len(lookback_df)
    brick_size = avg_range / 2

    print(f"[INFO] Total range: {total_range:.2f}")
    print(f"[INFO] Average range: {avg_range:.2f}")
    print(f"[INFO] Brick size (half of average range): {brick_size:.2f}")
    return brick_size


def remove_consecutive_duplicates(df: pd.DataFrame) -> pd.DataFrame:
    """
    Remove consecutive duplicate Renko bricks.

    Args:
        df: DataFrame with brick_low and brick_high columns

    Returns:
        DataFrame with consecutive duplicates removed
    """
    return df[
        (df["brick_low"] != df["brick_low"].shift())
        | (df["brick_high"] != df["brick_high"].shift())
    ]


def calculate_zone_trend(df: pd.DataFrame) -> pd.DataFrame:
    """
    Calculate trend and zone information for Renko bricks.

    Args:
        df: DataFrame with direction column

    Returns:
        DataFrame with trend column added
    """
    current_dir = "up"  # The first renko brick is always up.
    previous_uptrend = 0
    previous_downtrend = 0
    trend = 0

    for i, row in df.iterrows():
        # Bricks moving in the same direction
        if row["direction"] == current_dir:
            trend += 1
        # Brick changes the direction
        else:
            current_dir = row["direction"]
            # Preserve the previous trend
            if row["direction"] == "down":
                previous_uptrend = trend
            elif row["direction"] == "up":
                previous_downtrend = trend

            # Check if the current trend should resume from previous trend
            if row["direction"] == "up":
                if trend <= get_zone_from_trend(previous_uptrend):
                    trend = (previous_uptrend - trend) + 1
                else:
                    trend = 1
            elif row["direction"] == "down":
                if trend <= get_zone_from_trend(previous_downtrend):
                    trend = (previous_downtrend - trend) + 1
                else:
                    trend = 1
        df.at[i, "trend"] = trend

    print(f"[INFO] Generated {len(df)} Renko bricks")
    return df


def generate_renko_chart_data(df: pd.DataFrame, period_count: int) -> pd.DataFrame:
    """
    Generate Renko chart data from OHLCV data using VWAP.
    
    Args:
        df: DataFrame containing OHLCV data with 'vwap' and 'date' columns
        period_count: Number of recent periods to use for brick size calculation
        
    Returns:
        DataFrame containing Renko chart data
    """
    print(f"[INFO] Generating Renko chart with last {period_count} closed periods")
    brick_size = calculate_brick_size(df, period_count)
    renko_data = []
    current_price = df["vwap"].iloc[0]

    # Track contributing days for buyer_capital_ratio averaging
    contributing_days = []  # List of (date, buyer_capital_ratio) tuples
    last_brick_price = current_price

    for idx, row in df.iterrows():
        vwap_price = row["vwap"]
        current_date = row["date"]
        buyer_capital_ratio = row.get("buyer_capital_ratio", None)

        # Add current day to contributing days
        contributing_days.append((current_date, buyer_capital_ratio))

        bricks_generated_this_iteration = []

        # Generate up bricks
        while vwap_price >= current_price + brick_size:
            current_price += brick_size
            bricks_generated_this_iteration.append({
                "date": current_date,
                "brick_low": current_price - brick_size,
                "brick_high": current_price,
                "direction": "up",
                "trend": 0,
            })

        # Generate down bricks
        while vwap_price <= current_price - brick_size:
            current_price -= brick_size
            bricks_generated_this_iteration.append({
                "date": current_date,
                "brick_low": current_price,
                "brick_high": current_price + brick_size,
                "direction": "down",
                "trend": 0,
            })

        # If bricks were generated, assign buyer_capital_ratio
        if bricks_generated_this_iteration:
            # Calculate average buyer_capital_ratio from contributing days
            valid_ratios = [ratio for _, ratio in contributing_days if ratio is not None and not pd.isna(ratio)]
            avg_buyer_ratio = sum(valid_ratios) / len(valid_ratios) if valid_ratios else None

            # Assign the averaged ratio to all bricks generated
            for brick in bricks_generated_this_iteration:
                brick["buyer_capital_ratio"] = avg_buyer_ratio
                renko_data.append(brick)

            # Reset contributing days for next brick(s)
            contributing_days = []
            last_brick_price = current_price

    if not renko_data:
        print("[WARN] No Renko bricks generated.")
        return pd.DataFrame()

    renko_df = calculate_zone_trend(pd.DataFrame(renko_data))
    return renko_df


def load_csv_data(input_file: Path) -> pd.DataFrame:
    """
    Load CSV data and validate required columns.

    Args:
        input_file: Path to input CSV file

    Returns:
        DataFrame with loaded data

    Raises:
        ValueError: If required columns are missing
        FileNotFoundError: If input file doesn't exist
    """
    if not input_file.exists():
        raise FileNotFoundError(f"Input file not found: {input_file}")

    try:
        df = pd.read_csv(input_file)
        logger.info(f"Loaded {len(df):,} rows from {input_file}")
    except Exception as e:
        raise ValueError(f"Failed to read CSV file: {e}")

    # Validate required columns
    required_columns = ["date", "high", "low", "vwap", "buyer_capital_ratio"]
    missing_columns = [col for col in required_columns if col not in df.columns]
    if missing_columns:
        raise ValueError(f"Missing required columns: {missing_columns}")

    # Convert date column to datetime if it's not already
    if not pd.api.types.is_datetime64_any_dtype(df["date"]):
        try:
            df["date"] = pd.to_datetime(df["date"])
        except Exception as e:
            logger.warning(f"Could not convert date column to datetime: {e}")

    return df


def save_csv_data(df: pd.DataFrame, output_file: Path) -> None:
    """
    Save DataFrame to CSV file.

    Args:
        df: DataFrame to save
        output_file: Path to output CSV file
    """
    try:
        # Create output directory if it doesn't exist
        output_file.parent.mkdir(parents=True, exist_ok=True)
        df.to_csv(output_file, index=False)
        logger.info(f"Saved {len(df):,} rows to {output_file}")
    except Exception as e:
        raise ValueError(f"Failed to write CSV file: {e}")


def main():
    """Main function to process arguments and convert daily data to Renko."""
    parser = argparse.ArgumentParser(
        description="Convert daily OHLCV data to Renko chart data using VWAP",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument(
        "input_file",
        type=str,
        help="Path to input CSV file containing daily OHLCV data"
    )
    parser.add_argument(
        "output_file",
        type=str,
        help="Path to output CSV file for Renko data"
    )
    parser.add_argument(
        "--period-count",
        type=int,
        default=9,
        help="Number of recent periods to use for brick size calculation"
    )
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
        help="Set the logging level"
    )

    args = parser.parse_args()

    # Configure logging level
    logging.getLogger().setLevel(getattr(logging, args.log_level))

    try:
        # Convert paths
        input_file = Path(args.input_file).expanduser().resolve()
        output_file = Path(args.output_file).expanduser().resolve()

        logger.info(f"Input file: {input_file}")
        logger.info(f"Output file: {output_file}")
        logger.info(f"Period count: {args.period_count}")

        # Load data
        df = load_csv_data(input_file)

        # Generate Renko data
        renko_df = generate_renko_chart_data(df, args.period_count)

        if renko_df.empty:
            logger.error("No Renko data generated. Exiting.")
            sys.exit(1)

        # Save results
        save_csv_data(renko_df, output_file)

        logger.info("Conversion completed successfully!")

    except Exception as e:
        logger.error(f"Error during conversion: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()

# Example:
# python3 tick/daily_to_renko.py ./data/BTCUSDT-2025-1d.csv ./data/BTCUSDT-2025-1d-renko-vwap.csv
