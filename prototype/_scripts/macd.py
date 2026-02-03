import argparse
import os
import pandas as pd

from constants import MACD_FAST, MACD_SLOW, MACD_SIGNAL


def calculate_macd_weekly(df):
    print(f"[INFO] Resampling data to weekly timeframe...")

    df = df.copy()
    df.set_index("date", inplace=True)

    weekly = (
        df.resample("W")
        .agg(
            {
                "open": "first",
                "high": "max",
                "low": "min",
                "close": "last",
                "volume": "sum",
            }
        )
        .dropna()
    )

    print(f"[INFO] Calculating MACD on weekly data...")
    ema_fast = weekly["close"].ewm(span=MACD_FAST, adjust=False).mean()
    ema_slow = weekly["close"].ewm(span=MACD_SLOW, adjust=False).mean()
    macd = ema_fast - ema_slow
    macd_signal = macd.ewm(span=MACD_SIGNAL, adjust=False).mean()
    macd_histogram = macd - macd_signal

    weekly["macd_weekly"] = macd
    weekly["macd_signal_weekly"] = macd_signal
    weekly["macd_histogram_weekly"] = macd_histogram

    # Forward-fill weekly MACD back to daily
    weekly_macd = weekly[["macd_weekly", "macd_signal_weekly", "macd_histogram_weekly"]]
    df = df.merge(weekly_macd, how="left", left_index=True, right_index=True)
    df[["macd_weekly", "macd_signal_weekly", "macd_histogram_weekly"]] = df[
        ["macd_weekly", "macd_signal_weekly", "macd_histogram_weekly"]
    ].fillna(method="ffill")

    df.reset_index(inplace=True)
    print("[INFO] Weekly MACD columns added to daily data.")

    # Determine current state
    latest = df.dropna(subset=["macd_weekly", "macd_signal_weekly"]).iloc[-1]
    if latest["macd_weekly"] > latest["macd_signal_weekly"]:
        print("[STATE] ✅ Weekly MACD is in Positive Crossover State (Bullish)")
    elif latest["macd_weekly"] < latest["macd_signal_weekly"]:
        print("[STATE] ❌ Weekly MACD is in Negative Crossover State (Bearish)")
    else:
        print("[STATE] ⏸ Weekly MACD is in Neutral State")

    return df


def main():
    parser = argparse.ArgumentParser(
        description="Calculate MACD for a given ticker and timeframe."
    )
    parser.add_argument("--ticker", required=True, help="Ticker symbol (e.g., AAPL)")
    parser.add_argument(
        "--timeframe",
        required=True,
        choices=["daily", "weekly"],
        help="Timeframe (daily or weekly)",
    )

    args = parser.parse_args()

    ticker = args.ticker.upper()
    timeframe = args.timeframe.lower()

    input_file = f"{ticker}_{timeframe}.csv"
    output_file = f"{ticker}_{timeframe}_smm.csv"

    if not os.path.exists(input_file):
        print(f"[ERROR] File '{input_file}' does not exist.")
        return

    print(f"[INFO] Reading file: {input_file}")
    df = pd.read_csv(input_file, parse_dates=["date"])

    required_cols = {"date", "open", "high", "low", "close", "volume"}
    if not required_cols.issubset(df.columns):
        print(f"[ERROR] CSV must contain columns: {', '.join(required_cols)}")
        return

    if timeframe == "weekly":
        df = calculate_macd_weekly(df)
    else:
        print(f"[ERROR] Currently only 'weekly' MACD is supported in this version.")
        return

    df.to_csv(output_file, index=False)
    print(f"[INFO] Output written to: {output_file}")


if __name__ == "__main__":
    main()
