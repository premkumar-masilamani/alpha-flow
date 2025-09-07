import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from matplotlib.patches import Rectangle
import sys
import os

def load_data(csv_path):
    """Load and preprocess the input CSV."""
    df = pd.read_csv(csv_path)
    df['date'] = pd.to_datetime(df['date'])
    df = df.sort_values('date').reset_index(drop=True)
    return df

def generate_signals(df, strength_threshold=0.01, ratio_threshold=0.50, profit_threshold=0.10, vwap_exit_threshold=0.99):
    """Generate buy/sell/do-nothing signals with adjusted thresholds."""
    signals = []
    in_trade = False
    entry_price = 0.0

    for i, row in df.iterrows():
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
                (close_price < vwap or profit > profit_threshold)) or \
               (close_price < vwap * vwap_exit_threshold):  # Fallback exit if price drops significantly
                signals.append(-1)  # Sell
                in_trade = False
                entry_price = 0.0
            else:
                signals.append(0)  # Do nothing

    df['signal'] = signals
    return df

def plot_candlestick(ax, df):
    """Plot candlestick chart for OHLC data."""
    for i, row in df.iterrows():
        x = mdates.date2num(row['date'])
        open_price = row['open']
        close_price = row['close']
        high = row['high']
        low = row['low']

        color = 'green' if close_price >= open_price else 'red'
        height = abs(close_price - open_price)
        bottom = min(open_price, close_price)

        ax.add_patch(Rectangle((x - 0.2, bottom), 0.4, height, facecolor=color, edgecolor='black'))
        ax.vlines(x, low, high, color='black', linewidth=1)

def plot_vwap(ax, df):
    """Plot VWAP as a line overlay."""
    ax.plot(df['date'], df['vwap'], label='VWAP', color='blue', linestyle='--')

def plot_signals(ax, df):
    """Plot buy and sell signals as markers."""
    buy_signals = df[df['signal'] == 1]
    sell_signals = df[df['signal'] == -1]

    ax.scatter(buy_signals['date'], buy_signals['close'], marker='^', color='green', s=100, label='Buy Signal', zorder=5)
    ax.scatter(sell_signals['date'], sell_signals['close'], marker='v', color='red', s=100, label='Sell Signal', zorder=5)

def visualize_signals(csv_path, output_csv):
    """Main function to generate signals, visualize, and save output."""
    df = load_data(csv_path)

    # Generate signals with adjusted thresholds
    df = generate_signals(df)

    # Save updated CSV with signals
    df.to_csv(output_csv, index=False)
    print(f"Signals generated and written to {output_csv}")

    # Create visualization
    fig, ax = plt.subplots(figsize=(14, 8))
    plot_candlestick(ax, df)
    plot_vwap(ax, df)
    plot_signals(ax, df)

    ax.set_title('Daily Candlestick Chart with Buy/Sell Signals and VWAP')
    ax.set_xlabel('Date')
    ax.set_ylabel('Price')
    ax.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m-%d'))
    ax.xaxis.set_major_locator(mdates.AutoDateLocator())
    plt.setp(ax.get_xticklabels(), rotation=45)
    ax.grid(True)
    ax.legend()

    output_image = os.path.splitext(output_csv)[0] + '_candlestick_signals.png'
    plt.tight_layout()
    plt.savefig(output_image)
    print(f"Visualization saved to {output_image}")
    plt.show()

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python visualize_signals_updated.py <input_csv> <output_csv>")
        sys.exit(1)

    input_csv = sys.argv[1]
    output_csv = sys.argv[2]
    if not os.path.exists(input_csv):
        print(f"Error: Input file {input_csv} does not exist.")
        sys.exit(1)

    visualize_signals(input_csv, output_csv)

# Example:
# python3 tick/visualize_daily.py ./data/BTCUSDT-2025-1d.csv ./data/BTCUSDT-2025-1d-signals.csv
