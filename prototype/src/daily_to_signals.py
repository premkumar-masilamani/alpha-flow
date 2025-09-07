import pandas as pd
import sys
import os

def load_data(csv_path):
    """Load and preprocess the input CSV."""
    df = pd.read_csv(csv_path)
    df['date'] = pd.to_datetime(df['date'])
    df = df.sort_values('date').reset_index(drop=True)
    return df

def generate_signals(df, strength_threshold=0.05, ratio_threshold=0.55, profit_threshold=0.10):
    """Generate buy/sell/do-nothing signals based on the Whale Alignment Strategy."""
    signals = []
    in_trade = False
    entry_price = 0.0

    for i, row in df.iterrows():
        # Extract required metrics
        whale_net_direction = row['whale_net_direction']
        whale_bull_strength = row['whale_bull_strength']
        whale_bear_strength = row['whale_bear_strength']
        buyer_capital_ratio = row['buyer_capital_ratio']
        close_price = row['close']
        vwap = row['vwap']

        # Entry signal (buy)
        if not in_trade:
            if (whale_net_direction == 1 and
                whale_bull_strength > strength_threshold and
                buyer_capital_ratio > ratio_threshold and
                close_price > vwap):
                signals.append(1)  # Buy
                in_trade = True
                entry_price = close_price
            else:
                signals.append(0)  # Do nothing
        else:
            # Exit signal (sell)
            profit = (close_price - entry_price) / entry_price if entry_price > 0 else 0
            if (whale_net_direction == -1 and
                whale_bear_strength > strength_threshold and
                (close_price < vwap or profit > profit_threshold)):
                signals.append(-1)  # Sell
                in_trade = False
                entry_price = 0.0
            else:
                signals.append(0)  # Do nothing

    df['signal'] = signals
    return df

def main(input_csv, output_csv):
    """Main function to process CSV and generate signals."""
    # Load data
    df = load_data(input_csv)

    # Generate signals
    df = generate_signals(df)

    # Save to output CSV
    df.to_csv(output_csv, index=False)
    print(f"Signals generated and written to {output_csv}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python generate_signals.py <input_csv> <output_csv>")
        sys.exit(1)

    input_csv = sys.argv[1]
    output_csv = sys.argv[2]

    if not os.path.exists(input_csv):
        print(f"Error: Input file {input_csv} does not exist.")
        sys.exit(1)

    main(input_csv, output_csv)

# Example:
# python3 src/daily_to_signals.py ./BTCUSDT-2025-1d.csv ./BTCUSDT-2025-1d-signals.csv
