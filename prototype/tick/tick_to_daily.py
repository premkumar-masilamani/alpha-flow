import pandas as pd
import numpy as np
import zipfile
import os
import sys


def detect_whales(df, percentile=0.999, k_std=3):
    if df.empty:
        return pd.DataFrame([])

    perc_threshold = df["quote_qty"].quantile(percentile)
    mean = df["quote_qty"].mean()
    std = df["quote_qty"].std() if df["quote_qty"].std() > 0 else 0
    stat_threshold = mean + k_std * std if std > 0 else perc_threshold
    whale_threshold = max(perc_threshold, stat_threshold)

    whales = df[df["quote_qty"] >= whale_threshold]
    return whales


def process_file(filepath):
    basename = os.path.basename(filepath).replace(".zip", "")
    parts = basename.split("-")
    date_str = "-".join(parts[-3:])
    date = pd.to_datetime(date_str, format="%Y-%m-%d")

    # Load CSV from ZIP
    with zipfile.ZipFile(filepath, "r") as z:
        csv_file = [f for f in z.namelist() if f.endswith(".csv")][0]
        df = pd.read_csv(z.open(csv_file), header=None, names=[
            "trade_id", "price", "qty", "quote_qty", "timestamp", "is_buyer_maker", "ignore"
        ])

    # --- OHLCV ---
    ohlcv_open = df.iloc[0]["price"]
    ohlcv_close = df.iloc[-1]["price"]
    ohlcv_high = df["price"].max()
    ohlcv_low = df["price"].min()
    ohlcv_volume = df["qty"].sum()

    # --- Core metrics ---
    vwap = np.average(df["price"], weights=df["qty"])
    buyer_participation_ratio = (~df["is_buyer_maker"]).mean()
    buyer_capital_ratio = df.loc[~df["is_buyer_maker"], "quote_qty"].sum() / df["quote_qty"].sum()
    total_capital = df["quote_qty"].sum()

    # --- Whale detection ---
    whales = detect_whales(df)
    whale_buys = whales[~whales["is_buyer_maker"]]
    whale_sells = whales[whales["is_buyer_maker"]]

    whale_buy_capital = whale_buys["quote_qty"].sum()
    whale_sell_capital = whale_sells["quote_qty"].sum()
    whale_total_capital = whale_buy_capital + whale_sell_capital

    # --- Bullish / Bearish Whale Signal Strength ---
    whale_bull_strength = (whale_buy_capital / total_capital) * (whale_total_capital / total_capital) if total_capital > 0 else 0
    whale_bear_strength = (whale_sell_capital / total_capital) * (whale_total_capital / total_capital) if total_capital > 0 else 0

    # --- Net whale direction ---
    if whale_buy_capital > whale_sell_capital:
        whale_net_direction = 1
    elif whale_sell_capital > whale_buy_capital:
        whale_net_direction = -1
    else:
        whale_net_direction = 0

    return {
        "date": date,
        # OHLCV
        "open": ohlcv_open,
        "high": ohlcv_high,
        "low": ohlcv_low,
        "close": ohlcv_close,
        "volume": ohlcv_volume,
        # Derived metrics
        "vwap": vwap,
        "buyer_participation_ratio": buyer_participation_ratio,
        "buyer_capital_ratio": buyer_capital_ratio,
        "total_capital": total_capital,
        # Whale metrics
        "whale_buy_capital": whale_buy_capital,
        "whale_sell_capital": whale_sell_capital,
        "whale_bull_strength": whale_bull_strength,
        "whale_bear_strength": whale_bear_strength,
        "whale_net_direction": whale_net_direction
    }


def process_folder(data_folder, output_csv):
    results = []
    for file in sorted(os.listdir(data_folder)):
        if file.endswith(".zip"):
            zip_path = os.path.join(data_folder, file)
            print(f"Processing {zip_path}")
            results.append(process_file(zip_path))

    daily_df = pd.DataFrame(results)
    daily_df = daily_df.sort_values("date").reset_index(drop=True)
    daily_df.to_csv(output_csv, index=False)
    print(f"Daily data written to {output_csv}")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python tick_to_daily.py <data_folder> <output_csv>")
        sys.exit(1)

    data_folder = sys.argv[1]
    output_csv = sys.argv[2]
    process_folder(data_folder, output_csv)

# Example:
# python3 tick/tick_to_daily.py /Users/premkumar/Downloads/BTCUSDT-2025 ./data/BTCUSDT-2025-1d.csv
