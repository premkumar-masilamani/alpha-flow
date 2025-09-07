import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates

# -------------------------------
# Load CSV
# -------------------------------
csv_file = "BTCUSDT-2025-1d.csv"  # replace with your CSV
df = pd.read_csv(csv_file, parse_dates=["date"])
df.set_index("date", inplace=True)

# -------------------------------
# Normalize whale strengths
# -------------------------------
df["W_bull_norm"] = df["whale_bull_strength"] / df["whale_bull_strength"].max()
df["W_bear_norm"] = df["whale_bear_strength"] / df["whale_bear_strength"].max()
df["WM"] = df["W_bull_norm"] - df["W_bear_norm"]
df["WM_smooth"] = df["WM"].rolling(window=3, min_periods=1).mean()

# Entry/exit signals
df["buy_signal"] = df["whale_net_direction"] == 1
df["sell_signal"] = df["whale_net_direction"] == -1

# -------------------------------
# Candlestick plotting
# -------------------------------
fig, ax = plt.subplots(figsize=(16, 8))

# Convert dates to matplotlib format
dates = mdates.date2num(df.index.to_pydatetime())

# Width of candlestick body
width = 0.6
width2 = 0.1

for i, (date, row) in enumerate(df.iterrows()):
    # Candle color
    if row['close'] >= row['open']:
        color = 'green'
        lower = row['open']
        height = row['close'] - row['open']
    else:
        color = 'red'
        lower = row['close']
        height = row['open'] - row['close']

    # Draw candle body
    ax.add_patch(plt.Rectangle((dates[i]-width/2, lower), width, height, color=color))
    # Draw wicks
    ax.plot([dates[i], dates[i]], [row['low'], row['high']], color='black', linewidth=1)

# -------------------------------
# VWAP overlay
# -------------------------------
df['vwap'] = df['vwap']  # already computed in CSV
ax.plot(dates, df['vwap'], color='blue', linewidth=2, label='VWAP')

# -------------------------------
# Whale metrics overlay
# -------------------------------
ax.bar(dates, df["W_bull_norm"], width=0.5, color='green', alpha=0.3, label='Bullish Whale Strength')
ax.bar(dates, -df["W_bear_norm"], width=0.5, color='red', alpha=0.3, label='Bearish Whale Strength')

# Whale momentum line
ax.plot(dates, df["WM_smooth"], color='purple', linewidth=2, label='Whale Momentum (Smoothed)')

# Entry/exit arrows
ax.scatter(dates[df["buy_signal"]], df["close"][df["buy_signal"]],
           marker='^', color='lime', s=100, label='Buy Signal')
ax.scatter(dates[df["sell_signal"]], df["close"][df["sell_signal"]],
           marker='v', color='magenta', s=100, label='Sell Signal')

# Formatting
ax.xaxis_date()
ax.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m-%d'))
fig.autofmt_xdate()
ax.set_title("BTCUSDT Candlestick Chart with VWAP and Whale Signals")
ax.set_ylabel("Price / Whale Strength (Normalized)")
ax.legend(loc='upper left')
plt.show()
