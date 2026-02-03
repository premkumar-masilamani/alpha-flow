import requests
import pandas as pd
from datetime import datetime, timedelta
import time


def fetch_all_daily_btc_data():
    base_url = "https://api.exchange.coinbase.com/products/BTC-USD/candles"
    granularity = 86400  # 1 day
    max_days_per_call = 300
    earliest_start = datetime(2015, 1, 1)  # Coinbase BTC-USD trading began ~2015-01
    end_time = datetime.utcnow()

    all_data = []

    while earliest_start < end_time:
        next_end = earliest_start + timedelta(days=max_days_per_call)
        if next_end > end_time:
            next_end = end_time

        params = {
            "start": earliest_start.isoformat(),
            "end": next_end.isoformat(),
            "granularity": granularity,
        }

        print(f"Fetching: {earliest_start.date()} to {next_end.date()}...")
        response = requests.get(base_url, params=params)
        if response.status_code != 200:
            print(f"Failed request: {response.status_code}, {response.text}")
            break

        batch = response.json()
        if not batch:
            print("No more data returned.")
            break

        all_data.extend(batch)
        earliest_start = next_end
        time.sleep(0.35)  # Respectful delay to avoid rate-limiting

    # Parse into DataFrame
    df = pd.DataFrame(
        all_data, columns=["timestamp", "low", "high", "open", "close", "volume"]
    )
    df["timestamp"] = pd.to_datetime(df["timestamp"], unit="s")
    df.sort_values("timestamp", inplace=True)
    df.drop_duplicates("timestamp", inplace=True)

    return df


# Example usage
if __name__ == "__main__":
    df = fetch_all_daily_btc_data()
    df.to_csv("btc_usd_daily_full_coinbase.csv", index=False)
    print(
        f"Fetched {len(df)} daily candles. Saved to 'btc_usd_daily_full_coinbase.csv'."
    )
