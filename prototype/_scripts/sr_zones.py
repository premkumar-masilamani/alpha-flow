import argparse
import pandas as pd
from pathlib import Path
from typing import List, Tuple, Dict

from constants import (
    PIVOT_LOOKBACK,
    ZONE_TOLERANCE,
    TOUCHPOINT_DISTANCE,
    MIN_TOUCHES,
    RECENT_BARS,
    MAX_DEVIATION_FROM_PRICE,
)


def load_zones(data_dir: Path, ticker: str, timeframe: str) -> pd.DataFrame:
    zones_file = data_dir / f"{ticker}_{timeframe}_sr.csv"
    if not zones_file.exists():
        print(f"[WARN] Zone file not found: {zones_file}")
        return pd.DataFrame()

    df = pd.read_csv(zones_file)
    if not {"zone_lower", "zone_upper", "type"}.issubset(df.columns):
        print(f"[ERROR] Zone file missing required columns: {zones_file}")
        return pd.DataFrame()

    return df


def is_pivot_high(df: pd.DataFrame, i: int, n: int) -> bool:
    return all(
        df["high"].iloc[i] > df["high"].iloc[i - j]
        and df["high"].iloc[i] > df["high"].iloc[i + j]
        for j in range(1, n + 1)
    )


def is_pivot_low(df: pd.DataFrame, i: int, n: int) -> bool:
    return all(
        df["low"].iloc[i] < df["low"].iloc[i - j]
        and df["low"].iloc[i] < df["low"].iloc[i + j]
        for j in range(1, n + 1)
    )


def find_pivots(df: pd.DataFrame, n: int) -> Tuple[List[int], List[int]]:
    high_pivots, low_pivots = [], []
    for i in range(n, len(df) - n):
        if is_pivot_high(df, i, n):
            high_pivots.append(i)
        elif is_pivot_low(df, i, n):
            low_pivots.append(i)
    return high_pivots, low_pivots


def cluster_zones_with_dates(
        df: pd.DataFrame,
        pivot_indices: List[int],
        prices: List[float],
        tolerance: float,
        is_high: bool,
) -> List[Tuple[float, float, pd.Timestamp]]:
    if not prices:
        return []
    zones: List[Tuple[float, float, pd.Timestamp]] = []
    combined = sorted(zip(prices, pivot_indices), key=lambda x: x[0])
    cluster = [combined[0]]
    for price, idx in combined[1:]:
        if abs(price - cluster[-1][0]) / cluster[-1][0] <= tolerance:
            cluster.append((price, idx))
        else:
            zone_prices = [p for p, _ in cluster]
            zone_indices = [i for _, i in cluster]
            first_idx = min(zone_indices) if is_high else max(zone_indices)
            zones.append(
                (min(zone_prices), max(zone_prices), df["date"].iloc[first_idx])
            )
            cluster = [(price, idx)]
    zone_prices = [p for p, _ in cluster]
    zone_indices = [i for _, i in cluster]
    first_idx = min(zone_indices) if is_high else max(zone_indices)
    zones.append((min(zone_prices), max(zone_prices), df["date"].iloc[first_idx]))
    return zones


def count_touches(df: pd.DataFrame, zone: Tuple[float, float], column: str) -> int:
    lower, upper = zone
    touches = 0
    last_touch = -TOUCHPOINT_DISTANCE
    for i, price in enumerate(df[column]):
        if lower <= price <= upper and i - last_touch >= TOUCHPOINT_DISTANCE:
            touches += 1
            last_touch = i
    return touches


def zone_was_broken(
        df: pd.DataFrame, zone: Tuple[float, float], zone_type: str
) -> bool:
    recent = df.tail(RECENT_BARS)
    if zone_type == "resistance":
        return (recent["close"] > zone[1]).any()
    return (recent["close"] < zone[0]).any()


def detect_flips(
        support_zones: List[Tuple[float, float, pd.Timestamp]],
        resistance_zones: List[Tuple[float, float, pd.Timestamp]],
        df: pd.DataFrame,
) -> List[Dict]:
    flips: List[Dict] = []
    for s in support_zones:
        for r in resistance_zones:
            s_center = (s[0] + s[1]) / 2
            r_center = (r[0] + r[1]) / 2
            if abs(s_center - r_center) / s_center < ZONE_TOLERANCE:
                zone = (min(s[0], r[0]), max(s[1], r[1]))
                active_from = max(s[2], r[2])
                touches = max(
                    count_touches(df, zone, "low"), count_touches(df, zone, "high")
                )
                if (
                        touches >= MIN_TOUCHES
                        and not zone_was_broken(df, zone, "support")
                        and not zone_was_broken(df, zone, "resistance")
                ):
                    flips.append(
                        {
                            "zone": zone,
                            "touches": touches,
                            "type": "flip",
                            "active_from": active_from,
                        }
                    )
    return flips


def analyze_zones(df: pd.DataFrame) -> List[Dict]:
    high_idxs, low_idxs = find_pivots(df, PIVOT_LOOKBACK)
    resistance_prices = [df["high"].iloc[i] for i in high_idxs]
    support_prices = [df["low"].iloc[i] for i in low_idxs]
    resistance_zones = cluster_zones_with_dates(
        df, high_idxs, resistance_prices, ZONE_TOLERANCE, is_high=True
    )
    support_zones = cluster_zones_with_dates(
        df, low_idxs, support_prices, ZONE_TOLERANCE, is_high=False
    )

    results: List[Dict] = []
    current_price = df["close"].iloc[-1]

    for zone in resistance_zones:
        mid_price = (zone[0] + zone[1]) / 2
        if abs(mid_price - current_price) / current_price <= MAX_DEVIATION_FROM_PRICE:
            if not zone_was_broken(df, (zone[0], zone[1]), "resistance"):
                touches = count_touches(df, (zone[0], zone[1]), "high")
                if touches >= MIN_TOUCHES:
                    results.append(
                        {
                            "zone": (zone[0], zone[1]),
                            "touches": touches,
                            "type": "resistance",
                            "active_from": zone[2],
                        }
                    )

    for zone in support_zones:
        mid_price = (zone[0] + zone[1]) / 2
        if abs(mid_price - current_price) / current_price <= MAX_DEVIATION_FROM_PRICE:
            if not zone_was_broken(df, (zone[0], zone[1]), "support"):
                touches = count_touches(df, (zone[0], zone[1]), "low")
                if touches >= MIN_TOUCHES:
                    results.append(
                        {
                            "zone": (zone[0], zone[1]),
                            "touches": touches,
                            "type": "support",
                            "active_from": zone[2],
                        }
                    )

    results += detect_flips(support_zones, resistance_zones, df)
    results.sort(key=lambda x: (x["zone"][0] + x["zone"][1]) / 2, reverse=True)
    return results


def write_zones_to_csv(results: List[Dict], output_path: Path) -> None:
    df_out = pd.DataFrame(
        [
            {
                "type": r["type"],
                "zone_lower": round(r["zone"][0], 2),
                "zone_upper": round(r["zone"][1], 2),
                "touches": r["touches"],
                "active_from": r["active_from"],
            }
            for r in results
        ]
    )
    df_out.to_csv(output_path, index=False)


def main() -> None:
    parser = argparse.ArgumentParser(description="Support/Resistance Zone Detection")
    parser.add_argument(
        "--data-dir", required=True, help="Directory containing input data"
    )
    parser.add_argument("--ticker", required=True, help="Ticker symbol (e.g., BTC-USD)")
    parser.add_argument(
        "--timeframe", required=True, help="Timeframe string (e.g., 1d, 1h)"
    )
    args = parser.parse_args()

    data_dir = Path(args.data_dir)
    if not data_dir.exists():
        print(f"[ERROR] Data directory does not exist: {data_dir}")
        return

    input_file = data_dir / args.ticker / f"{args.ticker}_{args.timeframe}.csv"
    output_file = data_dir / args.ticker / f"{args.ticker}_{args.timeframe}_sr.csv"

    if not input_file.exists():
        print(f"[ERROR] Input file not found: {input_file}")
        return

    df = pd.read_csv(input_file, parse_dates=["date"])
    df.sort_values("date", inplace=True)
    df.reset_index(drop=True, inplace=True)

    print("[INFO] Analyzing support and resistance zones...")
    results = analyze_zones(df)

    print("\n[SUMMARY]")
    for r in results:
        print(
            f"{r['type'].upper()} | Zone: {r['zone']} | Touches: {r['touches']} | From: {r['active_from'].date()}"
        )

    write_zones_to_csv(results, output_file)
    print(f"[INFO] Zones written to {output_file}")


if __name__ == "__main__":
    main()
