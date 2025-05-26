import argparse
from pathlib import Path
import pandas as pd
import logging

logger = logging.getLogger(__name__)


def load_timeseries_data(file_path: Path) -> pd.DataFrame:
    """Load time series data from a CSV file.

    Args:
        file_path (Path): Path to the CSV file

    Returns:
        pd.DataFrame: DataFrame containing the time series data
    """
    try:
        df = pd.read_csv(file_path)
        logger.info(f"Loaded {len(df):,} existing rows from {file_path}")
        return df
    except Exception as e:
        logger.error(f"Failed to load data from {file_path}: {str(e)}")
        return pd.DataFrame()


def write_to_file(df: pd.DataFrame, file_path: Path) -> None:
    """Write DataFrame to a CSV file.

    Args:
        df (pd.DataFrame): DataFrame to save
        file_path (Path): Path where to save the file
    """
    try:
        file_path.parent.mkdir(parents=True, exist_ok=True)
        df.to_csv(file_path, index=False)
        logger.info(f"Saved {len(df):,} total rows to {file_path}")
    except Exception as e:
        logger.error(f"Failed to write data to {file_path}: {str(e)}")


def get_ohlcv_file_path(data_dir: str, ticker: str, timeframe: str) -> Path:
    """Generate path for OHLCV data file.

    Args:
        data_dir (str): Base directory for data storage
        ticker (str): Trading symbol
        timeframe (str): Time interval for the data

    Returns:
        Path: Full path to the OHLCV data file
    """
    data_path = Path(data_dir).expanduser().resolve()
    ticker_dir = data_path / ticker
    ticker_dir.mkdir(parents=True, exist_ok=True)
    return ticker_dir / f"{ticker}_{timeframe}.csv"


def get_renko_file_path(data_dir: str, ticker: str, timeframe: str) -> Path:
    """Generate path for Renko data file.

    Args:
        data_dir (str): Base directory for data storage
        ticker (str): Trading symbol
        timeframe (str): Time interval for the data

    Returns:
        Path: Full path to the Renko data file
    """
    data_path = Path(data_dir).expanduser().resolve()
    ticker_dir = data_path / ticker
    ticker_dir.mkdir(parents=True, exist_ok=True)
    return ticker_dir / f"{ticker}_{timeframe}_renko.csv"


def get_sr_file_path(data_dir: str, ticker: str, timeframe: str) -> Path:
    """Generate path for Support/Resistance data file.

    Args:
        data_dir (str): Base directory for data storage
        ticker (str): Trading symbol
        timeframe (str): Time interval for the data

    Returns:
        Path: Full path to the Support/Resistance data file
    """
    data_path = Path(data_dir).expanduser().resolve()
    ticker_dir = data_path / ticker
    sr_dir = ticker_dir / "sr"
    sr_dir.mkdir(parents=True, exist_ok=True)
    return sr_dir / f"{ticker}_{timeframe}_sr.csv"


def parse_chart_args() -> argparse.Namespace:
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description="Plot candlestick chart with SR zones using finplot."
    )
    parser.add_argument("--data-dir", default=".", help="Directory with CSV files")
    parser.add_argument("--ticker", required=True, help="Ticker symbol (e.g. AAPL)")
    parser.add_argument("--timeframe", required=True, help="Timeframe (e.g. 1d, 1h)")
    return parser.parse_args()

def get_zone_from_trend(trend: int) -> int:
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
