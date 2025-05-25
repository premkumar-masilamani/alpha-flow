import pandas as pd


def calculate_brick_size(df: pd.DataFrame, period_count: int) -> float:
    lookback_start = max(0, len(df) - period_count)
    lookback_df = df.iloc[lookback_start:]

    print("[INFO] Using the following periods for brick size calculation:")
    total_range = 0.0
    for idx, row in lookback_df.iterrows():
        high = row["high"]
        low = row["low"]
        rng = high - low
        total_range += rng
        print(
            f"  {row['date'].date()}: High = {high:.2f}, Low = {low:.2f}, Range = {rng:.2f}"
        )

    avg_range = total_range / len(lookback_df)
    brick_size = avg_range / 2

    print(f"[INFO] Total range: {total_range:.2f}")
    print(f"[INFO] Average range: {avg_range:.2f}")
    print(f"[INFO] Brick size (half of average range): {brick_size:.2f}")
    return brick_size


def remove_consecutive_duplicates(df: pd.DataFrame) -> pd.DataFrame:
    return df[
        (df["brick_low"] != df["brick_low"].shift())
        | (df["brick_high"] != df["brick_high"].shift())
    ]


def calculate_zone_trend(df: pd.DataFrame) -> pd.DataFrame:
    current_dir = "up"  # The first renko brick is always up.
    trend = 0
    zone = 0
    for i, row in df.iterrows():
        if row["direction"] == current_dir:
            trend += 1
            if trend <= 3:
                zone = 0
            elif trend <= 9:
                zone = 1
            elif trend <= 27:
                zone = 2
            elif trend <= 81:
                zone = 3
        else:
            current_dir = row["direction"]
            trend = 1
            zone = 0
        df.at[i, "trend"] = trend
        df.at[i, "zone"] = zone

    print(f"[INFO] Generated {len(df)} Renko bricks")
    return df


def generate_renko_chart_data(df: pd.DataFrame, period_count: int) -> pd.DataFrame:
    print(f"[INFO] Generating Renko chart with last {period_count} closed periods")
    brick_size = calculate_brick_size(df, period_count)
    renko_data = []
    current_price = df["close"].iloc[0]
    for idx, row in df.iterrows():
        close_price = row["close"]
        current_date = row["date"]

        while close_price >= current_price + brick_size:
            current_price += brick_size
            renko_data.append(
                {
                    "date": current_date,
                    "brick_low": current_price - brick_size,
                    "brick_high": current_price,
                    "direction": "up",
                    "zone": 0,
                    "trend": 0,
                }
            )

        while close_price <= current_price - brick_size:
            current_price -= brick_size
            renko_data.append(
                {
                    "date": current_date,
                    "brick_low": current_price,
                    "brick_high": current_price + brick_size,
                    "direction": "down",
                    "zone": 0,
                    "trend": 0,
                }
            )

    if not renko_data:
        print("[WARN] No Renko bricks generated.")
        return pd.DataFrame()

    renko_df = calculate_zone_trend(
        remove_consecutive_duplicates(pd.DataFrame(renko_data))
    )

    return renko_df
