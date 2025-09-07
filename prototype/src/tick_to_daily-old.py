import pandas as pd
import numpy as np
import zipfile
import os
import sys

def process_file(filepath):
    # Extract date from filename (BTCUSDT-trades-2025-01-02.zip → 2025-01-02)
    basename = os.path.basename(filepath).replace(".zip", "")
    parts = basename.split("-")
    date_str = "-".join(parts[-3:])   # ["2025","01","02"]
    date = pd.to_datetime(date_str, format="%Y-%m-%d")

    # Load data from zip
    with zipfile.ZipFile(filepath, "r") as z:
        csv_file = [f for f in z.namelist() if f.endswith(".csv")][0]
        df = pd.read_csv(z.open(csv_file), header=None, names=[
            "trade_id", "price", "qty", "quote_qty", "timestamp", "is_buyer_maker", "ignore"
        ])

    # daily metrics
    qty = df["qty"].sum()
    vwap = np.average(df["price"], weights=df["qty"])

    # How many trades were buyers?
    buyer_participation_ratio = (~df["is_buyer_maker"]).mean()

    # How much of the trading money was buyers?
    buyer_capital_ratio = df.loc[~df["is_buyer_maker"], "quote_qty"].sum() / df["quote_qty"].sum()

    # Impact of whale participation on market
    whale_impact = abs(buyer_capital_ratio - buyer_participation_ratio)

    return {
        "date": date,
        "qty": qty,
        "vwap": vwap,
        "buyer_capital_ratio": buyer_capital_ratio,
        "buyer_participation_ratio": buyer_participation_ratio,
        "whale_impact": whale_impact,
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

# python3 src/tick_to_daily.py /Users/premkumar/Downloads/BTCUSDT-2025 ./BTCUSDT-2025-1d.csv
