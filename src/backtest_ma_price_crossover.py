import pandas as pd
import numpy as np
from ta.trend import EMAIndicator, SMAIndicator, WMAIndicator
from constants import (
    INVESTMENT,
    MA_START,
    MA_END,
    OHLCV_FILE,
    MA_TYPES,
    COLUMN_NAME_MA,
    MA_FILE,
    MA_RESULTS_FILE,
)


def load_data(file_path: str) -> pd.DataFrame:
    print(f"Loading data from: {file_path}")
    df = pd.read_csv(file_path, parse_dates=["date"])
    print(f"Loaded {len(df)} rows.")
    return df


def calculate_hma(series: pd.Series, period: int) -> pd.Series:
    half_period = int(period / 2)
    sqrt_period = int(np.sqrt(period))
    wma_half = series.rolling(half_period).apply(
        lambda x: np.dot(x, np.arange(1, len(x) + 1))
        / np.sum(np.arange(1, len(x) + 1)),
        raw=True,
    )
    wma_full = series.rolling(period).apply(
        lambda x: np.dot(x, np.arange(1, len(x) + 1))
        / np.sum(np.arange(1, len(x) + 1)),
        raw=True,
    )
    hma = (
        (2 * wma_half - wma_full)
        .rolling(sqrt_period)
        .apply(
            lambda x: np.dot(x, np.arange(1, len(x) + 1))
            / np.sum(np.arange(1, len(x) + 1)),
            raw=True,
        )
    )
    return hma


def calculate_alma(
    series: pd.Series, period: int, offset: float = 0.85, sigma: float = 6
) -> pd.Series:
    def alma_filter(x):
        m = offset * (period - 1)
        s = period / sigma
        w = np.exp(-((np.arange(period) - m) ** 2) / (2 * s * s))
        w /= w.sum()
        return np.dot(w, x)

    return series.rolling(window=period).apply(alma_filter, raw=True)


def calculate_ma(df: pd.DataFrame, period: int, ma_type: str) -> pd.Series:
    close_series = pd.Series(df["close"])
    if ma_type == "sma":
        return SMAIndicator(close=close_series, window=period).sma_indicator()
    elif ma_type == "ema":
        return EMAIndicator(close=close_series, window=period).ema_indicator()
    elif ma_type == "wma":
        return WMAIndicator(close=close_series, window=period).wma()
    elif ma_type == "hma":
        return calculate_hma(close_series, period)
    elif ma_type == "alma":
        return calculate_alma(close_series, period)
    else:
        raise ValueError(f"Unsupported MA type: {ma_type}")


def backtest_strategy_vectorized(df: pd.DataFrame, period: int, ma_type: str) -> tuple:
    df = df.copy()
    df["ma"] = calculate_ma(df, period, ma_type)
    df.dropna(inplace=True)

    # Generate signal: 1 when price > MA, -1 otherwise
    df["signal"] = np.where(df["close"] > df["ma"], 1, -1)
    df["position"] = df["signal"].shift(1).fillna(0)

    # Calculate returns
    df["price_return"] = df["close"].pct_change()
    df["strategy_return"] = df["position"] * df["price_return"]
    df["cumulative_return"] = (1 + df["strategy_return"]).cumprod()

    final_value = INVESTMENT * df["cumulative_return"].iloc[-1]
    profit = round(final_value - INVESTMENT, 2)

    trades = int((df["position"] != df["position"].shift(1)).sum() / 2)

    print(f"[RESULT] {ma_type.upper()} {period}: Profit=${profit:.2f}, Trades={trades}")
    return ma_type, period, profit, trades


def backtest_range(df: pd.DataFrame, start: int, end: int, ma_type: str) -> list:
    print(f"Backtesting {ma_type.upper()} from period {start} to {end}...")
    return [
        backtest_strategy_vectorized(df, period, ma_type)
        for period in range(start, end + 1)
    ]


def save_results(results: list, file_path: str):
    df = pd.DataFrame(
        data=results, columns=[COLUMN_NAME_MA, "period", "profit", "trades"]
    )
    df.to_csv(file_path, index=False)
    print(f"Saved {len(results)} results to {file_path}")


def analyze_moving_averages():
    df = pd.read_csv(MA_FILE, usecols=["ma", "period", "profit", "trades"])
    df.dropna(subset=["period", "profit", "trades"], inplace=True)
    df[["period", "profit", "trades"]] = df[["period", "profit", "trades"]].apply(
        pd.to_numeric, errors="coerce"
    )

    # Derived metrics
    df["profit_per_trade"] = df["profit"] / df["trades"].replace(0, pd.NA)
    df.dropna(subset=["profit_per_trade"], inplace=True)

    # Normalize metrics
    norm_cols = {
        "profit_norm": "profit",
        "profit_per_trade_norm": "profit_per_trade",
        "trades_norm": "trades",
    }
    for norm_col, source in norm_cols.items():
        min_val, max_val = df[source].min(), df[source].max()
        if min_val != max_val:
            df[norm_col] = (df[source] - min_val) / (max_val - min_val)
        else:
            df[norm_col] = 0.0

    df["trades_norm"] = 1 - df["trades_norm"]  # Prefer fewer trades

    # Scoring
    df["score"] = (
        0.5 * df["profit_norm"]
        + 0.3 * df["profit_per_trade_norm"]
        + 0.2 * df["trades_norm"]
    )

    # Range bucketing
    range_labels = ["1-week", "2-weeks", "1-month", "3-months", "6-months", "1-year"]
    range_bins = [0, 7, 14, 31, 92, 184, 366]
    df["range"] = pd.cut(df["period"], bins=range_bins, labels=range_labels)
    df["range"] = pd.Categorical(df["range"], categories=range_labels, ordered=True)

    # Local score smoothing using rolling window (centered ±3)
    df.sort_values("period", inplace=True)
    df["local_score"] = df["score"].rolling(window=7, center=True, min_periods=1).mean()
    df["final_score"] = 0.6 * df["score"] + 0.4 * df["local_score"]

    # Select top period per MA type per range
    top_ma = (
        df.sort_values("final_score", ascending=False)
        .groupby(["ma", "range"], observed=True)
        .first()
        .reset_index()
    )

    # Final selection
    result = top_ma[["ma", "range", "period"]].round(2)
    print("Top MA Periods per Range:")
    print(result)

    result.to_csv(MA_RESULTS_FILE, index=False)
    print(f"Results saved to {MA_RESULTS_FILE}")


def main():
    df = load_data(str(OHLCV_FILE))
    all_results = []

    for ma_type in MA_TYPES:
        all_results += backtest_range(df, MA_START, MA_END, ma_type)

    save_results(all_results, str(MA_FILE))

    analyze_moving_averages()


if __name__ == "__main__":
    main()
