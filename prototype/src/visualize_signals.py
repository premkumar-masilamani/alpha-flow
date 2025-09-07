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

        # Candle body
        ax.add_patch(Rectangle((x - 0.2, bottom), 0.4, height, facecolor=color, edgecolor='black'))

        # Wick
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

def visualize_signals(csv_path):
    """Main function to create candlestick chart with signals."""
    df = load_data(csv_path)

    fig, ax = plt.subplots(figsize=(14, 8))

    # Plot candlestick, VWAP, and signals
    plot_candlestick(ax, df)
    plot_vwap(ax, df)
    plot_signals(ax, df)

    # Customize plot
    ax.set_title('Daily Candlestick Chart with Buy/Sell Signals and VWAP')
    ax.set_xlabel('Date')
    ax.set_ylabel('Price')
    ax.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m-%d'))
    ax.xaxis.set_major_locator(mdates.AutoDateLocator())
    plt.setp(ax.get_xticklabels(), rotation=45)
    ax.grid(True)
    ax.legend()

    # Save and display
    output_image = os.path.splitext(csv_path)[0] + '_candlestick_signals.png'
    plt.tight_layout()
    plt.savefig(output_image)
    print(f"Visualization saved to {output_image}")
    plt.show()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python visualize_signals.py <input_csv>")
        sys.exit(1)

    input_csv = sys.argv[1]
    if not os.path.exists(input_csv):
        print(f"Error: Input file {input_csv} does not exist.")
        sys.exit(1)

    visualize_signals(input_csv)

# Example:
# python3 src/visualize_signals.py ./BTCUSDT-2025-1d-signals.csv
