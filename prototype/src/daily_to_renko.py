import pandas as pd

INPUT_FILE = "./BTCUSDT-2025-1d.csv"
OUTPUT_FILE = "./BTCUSDT-2025-renko.csv"

def build_renko(df: pd.DataFrame, brick_size: float) -> pd.DataFrame:
    """
    Build Renko bars based on VWAP.
    Overlay additional metrics (buyer participation, capital, volume ratios, whale impact).
    """
    renko_bars = []
    last_price = df["vwap"].iloc[0]
    cumulative_move = 0.0
    direction = 0  # +1 up, -1 down

    for _, row in df.iterrows():
        price = row["vwap"]
        move = price - last_price
        cumulative_move += move

        # Check if enough move to form bricks
        while abs(cumulative_move) >= brick_size:
            if cumulative_move > 0:
                direction = 1
                last_price += brick_size
                cumulative_move -= brick_size
            else:
                direction = -1
                last_price -= brick_size
                cumulative_move += brick_size

            renko_bars.append({
                "date": row["date"],
                "price": last_price,
                "direction": direction,
                "buyer_participation_ratio": row["buyer_participation_ratio"],
                "buyer_capital_ratio": row["buyer_capital_ratio"],
                "whale_impact": row["whale_impact"],
            })

    return pd.DataFrame(renko_bars)

def calculate_brick_size(df: pd.DataFrame, lookback: int = 9) -> float:
    """
    Calculate Renko brick size using VWAP absolute ranges.
    """
    df = df.copy()
    df["vwap_range"] = df["vwap"].diff().abs()
    brick_size = df["vwap_range"].rolling(lookback).mean().iloc[-1]
    return float(brick_size)

def main():
    # Load Program 1 output
    df = pd.read_csv(INPUT_FILE, parse_dates=["date"])

    # Brick size based on VWAP changes
    brick_size = calculate_brick_size(df, lookback=9)
    print(f"Renko brick size: {brick_size:.2f}")

    # Build Renko bars
    renko_df = build_renko(df, brick_size)

    # Save
    renko_df.to_csv(OUTPUT_FILE, index=False)
    print(f"Saved Renko data to {OUTPUT_FILE}")

if __name__ == "__main__":
    main()

# python3 src/daily_to_renko.py
