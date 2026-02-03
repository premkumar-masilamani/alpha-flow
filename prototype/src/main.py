import argparse
import json
import logging
from pathlib import Path
from typing import List, Dict, Any

from candle_data import get_next_timeframe, process_ticker
from helpers import get_candle_file_path, get_renko_file_path, get_renko_ma_file_path, write_to_file
from renko_data import generate_renko_chart_data
from renko_ma import calculate_moving_averages

logger = logging.getLogger(__name__)


def load_config(config_path: str) -> Dict[str, Any]:
    """
    Load and validate configuration from a JSON file.

    Args:
        config_path: Path to the configuration file

    Returns:
        Dictionary containing the validated configuration

    Raises:
        FileNotFoundError: If the config file doesn't exist
        ValueError: If required fields are missing
        json.JSONDecodeError: If the JSON is malformed
    """
    config_file = Path(config_path).expanduser().resolve()

    if not config_file.exists():
        raise FileNotFoundError(f"Config file not found: {config_file}")

    try:
        with open(config_file, "r") as f:
            config = json.load(f)
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON in config file: {e}")

    return config


def run(config_path: str):
    """
    Main execution function for the application.

    Args:
        config_path: Path to the configuration file

    Returns:
        True if execution was successful, False otherwise
    """
    # Load the configurations
    try:
        config = load_config(config_path)
    except Exception as e:
        logger.error(f"Failed to load config: {str(e)}")
        return False

    try:
        data_dir_path = Path(config["data_dir"]).expanduser().resolve()
        data_dir_path.mkdir(parents=True, exist_ok=True)
        data_dir: str = str(data_dir_path)
    except Exception as e:
        logger.error(f"Failed to access or create data directory: {str(e)}")
        return False

    tickers: List[str] = [t.upper() for t in config.get("tickers", [])]
    if not tickers:
        logger.warning("No tickers provided in config. Exiting.")
        return False

    timeframe: str = config.get("timeframe", "1d").lower()
    renko_period_count = config.get("renko", {}).get("period_count", 9)

    logger.info(f"Data directory: {data_dir}")
    logger.info(f"Tickers: {', '.join(tickers)}")
    logger.info(f"Timeframe: {timeframe}")
    logger.info(f"Renko Period Count: {renko_period_count}")

    # Main Trading Logic
    next_timeframe = get_next_timeframe(timeframe)
    total_tickers = len(tickers)
    for i, ticker in enumerate(tickers):
        logger.info(f"Processing {ticker} ({i + 1}/{total_tickers})")

        try:
            # Step 1a: Download OHLCV Data for current timeframe (wave)
            current_df = process_ticker(data_dir, ticker, timeframe)
            write_to_file(current_df, get_candle_file_path(data_dir, ticker, timeframe))

            # Step 1b: Download OHLCV Data for next timeframe (tide)
            if next_timeframe:
                next_df = process_ticker(data_dir, ticker, next_timeframe)
                write_to_file(
                    next_df, get_candle_file_path(data_dir, ticker, next_timeframe)
                )

            # Step 1c: Compute Renko Bricks for current timeframe (wave)
            renko_df = generate_renko_chart_data(current_df, renko_period_count)
            write_to_file(renko_df, get_renko_file_path(data_dir, ticker, timeframe))

            # Step 2b: Compute Moving Averages for OHLCV data
            # TODO: Write the code to compute moving averages for OHLCV data

            # Step 2b: Compute Moving Averages for Renko
            sma_periods = config["ma"]["sma"]["periods"]
            ema_periods = config["ma"]["ema"]["periods"]
            print(f"type = {type(sma_periods)}")
            renko_ma_df = calculate_moving_averages(renko_df, sma_periods, ema_periods)
            write_to_file(renko_ma_df, get_renko_ma_file_path(data_dir, ticker, timeframe))

            logger.info(f"Processed {ticker} at {timeframe} timeframe ")
        except Exception as e:
            logger.error(f"Failed to process {ticker}: {str(e)}")
            continue


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="ASTA - SMM Trading Strategy")
    parser.add_argument("--config", required=True, help="Path to config YAML file")
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
        help="Set the logging level",
    )
    args = parser.parse_args()

    # Configure logging
    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    run(args.config)
