import time
import logging
from typing import Optional, Dict
import pandas as pd
import yfinance as yf
from datetime import datetime
from functools import lru_cache

from helpers import (
    load_timeseries_data,
    get_ohlcv_file_path,
)

logger = logging.getLogger(__name__)

RATE_LIMIT_DELAY_SECONDS = 3

# Mapping timeframes to their next level and equivalent days
TIMEFRAME_DAY_MAPPING: Dict[str, Dict[str, Optional[str]]] = {
    "1d": {"next": "1wk", "days": "1"},
    "1wk": {"next": "1mo", "days": "7"},
    "1mo": {"next": None, "days": "30"},
}

# Default start date for empty dataframes
DEFAULT_START_DATE = datetime(1900, 1, 1).date()


@lru_cache(maxsize=32)
def get_next_timeframe(timeframe: str) -> Optional[str]:
    """Get the next larger timeframe or None if at largest timeframe."""
    return TIMEFRAME_DAY_MAPPING.get(timeframe, {}).get("next")


@lru_cache(maxsize=32)
def get_timeframe_days(timeframe: str) -> Optional[str]:
    """Get the number of days equivalent to the timeframe."""
    return TIMEFRAME_DAY_MAPPING.get(timeframe, {}).get("days")


def get_next_start_date(df: pd.DataFrame, timeframe: str) -> Optional[str]:
    """
    Calculate the next start date for data download based on the last date in the dataframe.
    Returns None if data is already up to date.
    """
    # If dataframe is empty, start from a long time ago
    if df.empty:
        last_date = DEFAULT_START_DATE
    else:
        # Ensure 'date' column is in datetime format
        df["date"] = pd.to_datetime(df["date"], errors="coerce")
        last_date = df["date"].max().date()

    # Calculate the next date based on the timeframe
    days = int(get_timeframe_days(timeframe) or 0)
    next_date = last_date + pd.Timedelta(days=days)

    # Only return a date if it's in the past
    if next_date < datetime.now().date():
        return next_date.strftime("%Y-%m-%d")
    return None


def download_data(ticker: str, timeframe: str, start_date: str) -> pd.DataFrame:
    """
    Download stock data from Yahoo Finance and process into a standardized format.
    Returns an empty DataFrame if download fails.
    """
    logger.info(f"Downloading {ticker} from {start_date} with '{timeframe}' timeframe")

    # Create empty DataFrame as fallback
    empty_df = pd.DataFrame(columns=["date", "open", "high", "low", "close", "volume"])

    try:
        # Make the API call to Yahoo Finance
        df = yf.download(
            ticker,
            start=start_date,
            interval=timeframe,
            progress=False,
            auto_adjust=False,
        )

        # Respect rate limiting to avoid API blocks
        time.sleep(RATE_LIMIT_DELAY_SECONDS)

        if df is not None and df.empty:
            logger.info(f"No data available for {ticker} from {start_date}")
            return empty_df

    except Exception as e:
        logger.error(f"Failed to download data for {ticker}: {e}")
        return empty_df

    try:
        # Handle multi-level column indices that YFinance sometimes returns
        if isinstance(df.columns, pd.MultiIndex):
            df.columns = df.columns.get_level_values(0)

        # Optimize the DataFrame operations
        df = df.reset_index()

        # Convert columns to lowercase once
        df.columns = [col.lower() for col in df.columns]

        # Select only the columns we need and drop NA values
        required_cols = ["date", "open", "high", "low", "close", "volume"]
        df = df[required_cols].dropna()

        # Ensure date column is in consistent datetime format without time component
        df["date"] = pd.to_datetime(df["date"]).dt.normalize()

        logger.info(f"Retrieved {len(df)} rows for {ticker} at interval '{timeframe}'")
        return df

    except Exception as e:
        logger.error(f"Failed to process data for {ticker}: {e}")
        return empty_df


def process_ticker(data_dir: str, ticker: str, timeframe: str) -> pd.DataFrame:
    """
    Process a ticker for the given timeframe.
    Loads existing data, downloads new data if needed, and combines them.
    Returns True if new data was added, False otherwise.
    """
    file_path = get_ohlcv_file_path(data_dir, ticker, timeframe)
    existing_df = load_timeseries_data(file_path)

    next_start_date = get_next_start_date(existing_df, timeframe)
    if next_start_date is None:
        logger.info(f"No new data for {ticker} at '{timeframe}' — up to date.")
        return existing_df

    # Download new data
    new_df = download_data(ticker, timeframe, next_start_date)
    if new_df.empty:
        logger.info(f"No new data for {ticker} at '{timeframe}' — up to date.")
        return existing_df

    # Reset index before combining
    existing_df = existing_df.reset_index(drop=True)
    new_df = new_df.reset_index(drop=True)

    # Update existing data with new data (faster than concat in many cases)
    combined_df = new_df.combine_first(existing_df)

    # Sort by date
    combined_df.sort_values("date", inplace=True)
    combined_df.reset_index(drop=True, inplace=True)
    logger.info(f"Updated {ticker} ({timeframe}) with {len(new_df)} new records")

    return combined_df
