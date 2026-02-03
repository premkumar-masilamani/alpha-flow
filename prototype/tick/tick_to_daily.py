import numpy as np
import os
import pandas as pd
import sys
import zipfile


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
    open_ = df.iloc[0]["price"]
    close = df.iloc[-1]["price"]
    high = df["price"].max()
    low = df["price"].min()
    volume = df["qty"].sum()
    total_quote_qty = df["quote_qty"].sum()
    # --- VWAP ---
    vwap = total_quote_qty / volume if volume > 0 else 0
    # --- Buyer/Seller Pressure ---
    buyer_volume = df.loc[~df["is_buyer_maker"], "qty"].sum()
    seller_volume = df.loc[df["is_buyer_maker"], "qty"].sum()
    buyer_capital = df.loc[~df["is_buyer_maker"], "quote_qty"].sum()
    buyer_capital_ratio = buyer_capital / total_quote_qty if total_quote_qty > 0 else 0
    buyer_volume_ratio = (buyer_volume - seller_volume) / volume if volume > 0 else 0
    # --- Time & Trade Dynamics ---
    min_time = df["timestamp"].min()
    max_time = df["timestamp"].max()
    duration_sec = (max_time - min_time) / 1000.0
    trades_per_sec = len(df) / duration_sec if duration_sec > 0 else 0
    time_diffs = df["timestamp"].diff().dropna()
    avg_inter_trade_ms = time_diffs.mean() if not time_diffs.empty else 0
    # --- Micro Volatility ---
    micro_volatility = 0.0
    prices = df["price"].values
    if len(prices) > 1:
        returns = np.array([(prices[i] - prices[i - 1]) / prices[i - 1] if prices[i - 1] != 0 else 0 for i in range(1, len(prices))])
        if len(returns) > 0:
            micro_volatility = np.std(returns)
    # --- VPIN Proxy ---
    vpin = abs(buyer_volume - seller_volume) / volume if volume > 0 else 0
    return {
        "date": date,
        "open": open_,
        "high": high,
        "low": low,
        "close": close,
        "volume": volume,
        "vwap": vwap,
        "buyer_capital_ratio": buyer_capital_ratio,
        "buyer_volume_ratio": buyer_volume_ratio,
        "trades_per_sec": trades_per_sec,
        "micro_volatility": micro_volatility,
        "avg_inter_trade_ms": avg_inter_trade_ms,
        "vpin": vpin,
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
