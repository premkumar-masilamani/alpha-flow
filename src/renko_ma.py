import argparse
from pathlib import Path
import pandas as pd
import logging
import json

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


def get_renko_ma_file_path(data_dir: str, ticker: str, timeframe: str) -> Path:
    """Generate path for Renko data with moving averages file.

    Args:
        data_dir (str): Base directory for data storage
        ticker (str): Trading symbol
        timeframe (str): Time interval for the data

    Returns:
        Path: Full path to the Renko data with moving averages file
    """
    data_path = Path(data_dir).expanduser().resolve()
    ticker_dir = data_path / ticker
    ticker_dir.mkdir(parents=True, exist_ok=True)
    return ticker_dir / f"{ticker}_{timeframe}_renko_ma.csv"


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


def calculate_moving_averages(ticker: str, timeframe: str, config_path: str = "technical-analysis/config/config.json") -> None:
    """
    Calculates Simple Moving Averages (SMA) and Exponential Moving Averages (EMA) for Renko data
    and saves the results to a CSV file.

    Args:
        ticker (str): The ticker symbol (e.g., "BTC-USD").
        timeframe (str): The timeframe for the data (e.g., "1d").
        config_path (str, optional): The path to the configuration file.
            Defaults to "technical-analysis/config/config.json".
    """

    try:
        # Load configuration
        with open(config_path, "r") as f:
            config = json.load(f)

        data_dir = config["data_dir"]
        sma_periods = config["ma"]["sma"]["periods"]
        ema_periods = config["ma"]["ema"]["periods"]

        # Construct file paths
        renko_file_path = get_renko_file_path(data_dir, ticker, timeframe)
        output_file_path = get_renko_ma_file_path(data_dir, ticker, timeframe)

        # Load Renko data
        renko_df = load_timeseries_data(renko_file_path)
        if renko_df.empty:
            logger.warning(
                f"No data loaded for {ticker} {timeframe} from {renko_file_path}.  Exiting."
            )
            return

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

        output_df = renko_df[output_columns]

        # Save the results
        write_to_file(output_df, output_file_path)

    except FileNotFoundError:
        logger.error(f"Config file not found at {config_path}")
    except KeyError as e:
        logger.error(f"Missing key in config file: {e}")
    except Exception as e:
        logger.error(f"An error occurred: {e}")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Calculate SMA and EMA for Renko charts."
    )
    parser.add_argument("--ticker", required=True, help="Ticker symbol (e.g., BTC-USD)")
    parser.add_argument("--timeframe", required=True, help="Timeframe (e.g., 1d)")
    parser.add_argument(
        "--config", default="technical-analysis/config/config.json", help="Path to config.json"
    )

    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
    )

    calculate_moving_averages(args.ticker, args.timeframe, args.config)