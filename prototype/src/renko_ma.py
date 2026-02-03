import json
import logging
import pandas as pd
from numpy import int8
from pathlib import Path

logger = logging.getLogger(__name__)


def calculate_moving_averages(renko_df: pd.DataFrame, sma_periods: list[int], ema_periods: list[int]) -> pd.DataFrame:
    # Use brick_high for up bricks and brick_low for down bricks as closing price
    renko_df["close"] = renko_df.apply(
        lambda row: row["brick_high"]
        if row["direction"] == "up"
        else row["brick_low"],
        axis=1,
    )

    # Calculate SMA
    for period in sma_periods:
        renko_df[f"sma_{period}"] = renko_df["close"].rolling(window=period).mean()

    # Calculate EMA
    for period in ema_periods:
        renko_df[f"ema_{period}"] = renko_df["close"].ewm(span=period).mean()

    # Select and reorder columns for output
    output_columns = ["date"]
    output_columns.extend([f"sma_{period}" for period in sma_periods])
    output_columns.extend([f"ema_{period}" for period in ema_periods])

    return renko_df[output_columns]
