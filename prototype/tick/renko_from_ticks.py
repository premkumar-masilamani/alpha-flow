import os
import zipfile
import pandas as pd
import numpy as np

LOOKBACK = 9  # hardcoded lookback for brick size

# ---------------------------
# Step 1: Load Tick Data (supports ZIP)
# ---------------------------
def load_tick_data(directory):
    dfs = []
    for file in os.listdir(directory):
        path = os.path.join(directory, file)
        if file.endswith(".csv"):
            df = pd.read_csv(path)
            dfs.append(df)
        elif file.endswith(".zip"):
            with zipfile.ZipFile(path, "r") as z:
                for name in z.namelist():
                    if name.endswith(".csv"):
                        with z.open(name) as f:
                            df = pd.read_csv(f)
                            dfs.append(df)
    if not dfs:
        raise ValueError("No tick data files (csv/zip) found in directory")
    tick_df = pd.concat(dfs, ignore_index=True)
    tick_df["timestamp"] = pd.to_datetime(tick_df["timestamp"])
    tick_df = tick_df.sort_values("timestamp").reset_index(drop=True)
    return tick_df


# ---------------------------
# Step 2: Build Daily OHLCV
# ---------------------------
def ticks_to_daily(tick_df):
    tick_df["date"] = tick_df["timestamp"].dt.date
    ohlcv = tick_df.groupby("date").agg(
        open=("price", "first"),
        high=("price", "max"),
        low=("price", "min"),
        close=("price", "last"),
        volume=("qty", "sum"),
        quote_volume=("quote_qty", "sum"),
    ).reset_index()
    return ohlcv


# ---------------------------
# Step 3: Compute Brick Sizes
# ---------------------------
def compute_brick_sizes(ohlcv, lookback=LOOKBACK):
    ohlcv["range"] = ohlcv["high"] - ohlcv["low"]
    ohlcv["brick_size"] = ohlcv["range"].rolling(lookback, min_periods=1).mean() / 2.0
    return ohlcv.set_index("date")


# ---------------------------
# Step 4: Build Renko Bricks
# ---------------------------
def build_dynamic_renko(tick_df, ohlcv):
    tick_df["date"] = tick_df["timestamp"].dt.date
    tick_df["brick_size"] = tick_df["date"].map(ohlcv["brick_size"])
    tick_df = tick_df.sort_values("timestamp").reset_index(drop=True)

    last_close = float(tick_df.loc[0, "price"])
    bricks = []
    pending_ticks = []

    def flush_brick(direction, open_p, close_p, ticks_for_brick):
        df_ticks = pd.DataFrame(ticks_for_brick)
        if df_ticks.empty:
            buyer_capital = seller_capital = buyer_volume = seller_volume = 0.0
        else:
            buyer_mask = ~df_ticks["is_buyer_maker"].astype(bool)
            buyer_capital = df_ticks.loc[buyer_mask, "quote_qty"].sum()
            seller_capital = df_ticks.loc[~buyer_mask, "quote_qty"].sum()
            buyer_volume = df_ticks.loc[buyer_mask, "qty"].sum()
            seller_volume = df_ticks.loc[~buyer_mask, "qty"].sum()
        total_quote = buyer_capital + seller_capital
        buyer_capital_ratio = buyer_capital / total_quote if total_quote > 0 else 0.5
        volume = buyer_volume + seller_volume
        buyer_volume_ratio = (buyer_volume - seller_volume) / volume if volume > 0 else 0.0

        bricks.append({
            "open": open_p,
            "close": close_p,
            "direction": "up" if direction > 0 else "down",
            "buyer_capital": buyer_capital,
            "seller_capital": seller_capital,
            "buyer_capital_ratio": buyer_capital_ratio,
            "buyer_volume": buyer_volume,
            "seller_volume": seller_volume,
            "buyer_volume_ratio": buyer_volume_ratio,
            "tick_count": len(df_ticks),
            "start_time": df_ticks["timestamp"].min() if not df_ticks.empty else None,
            "end_time": df_ticks["timestamp"].max() if not df_ticks.empty else None,
        })

    for _, tick in tick_df.iterrows():
        price = float(tick["price"])
        brick_size = float(tick["brick_size"])
        pending_ticks.append(tick.to_dict())

        while True:
            diff = price - last_close
            if diff >= brick_size:
                flush_brick(1, last_close, last_close + brick_size, pending_ticks.copy())
                pending_ticks.clear()
                last_close += brick_size
                continue
            if diff <= -brick_size:
                flush_brick(-1, last_close, last_close - brick_size, pending_ticks.copy())
                pending_ticks.clear()
                last_close -= brick_size
                continue
            break

    return pd.DataFrame(bricks)


# ---------------------------
# Step 5: Main
# ---------------------------
def main(input_dir, ohlcv_out, renko_out):
    tick_df = load_tick_data(input_dir)
    ohlcv = ticks_to_daily(tick_df)
    ohlcv_with_bricks = compute_brick_sizes(ohlcv, LOOKBACK)

    # Save OHLCV
    ohlcv.to_csv(ohlcv_out, index=False)

    renko_df = build_dynamic_renko(tick_df, ohlcv_with_bricks)
    renko_df.to_csv(renko_out, index=False)

    print(f"OHLCV data saved to {ohlcv_out}")
    print(f"Renko data saved to {renko_out}")


if __name__ == "__main__":
    import sys
    if len(sys.argv) < 4:
        print("Usage: python renko_from_ticks.py <tick_data_directory> <ohlcv_output.csv> <renko_output.csv>")
        sys.exit(1)
    input_dir, ohlcv_out, renko_out = sys.argv[1:4]
    main(input_dir, ohlcv_out, renko_out)

# Example:
# python3 tick/renko_from_ticks.py /Users/premkumar/Downloads/BTCUSDT-2025 ./data/BTCUSDT-2025-1d.csv ./data/BTCUSDT-2025-1d-renko.csv
