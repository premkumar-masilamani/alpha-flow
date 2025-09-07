import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import matplotlib.colors as mcolors

# === Load Renko data ===
df = pd.read_csv("./BTCUSDT-2025-renko.csv", parse_dates=["date"])

# Color mapping for buyer_capital_ratio
norm = mcolors.Normalize(vmin=float(df["buyer_capital_ratio"].min()),
                         vmax=float(df["buyer_capital_ratio"].max()))
cmap = plt.colormaps['RdYlGn']

# === Plot setup ===
fig, axes = plt.subplots(3, 1, figsize=(14, 10), sharex=True,
                         gridspec_kw={'height_ratios': [3, 1, 1]})

# ---------------------------
# 1. Diagonal Renko Bricks Colored by Buyer Capital Ratio
# ---------------------------
ax = axes[0]

# Initialize previous price for brick height calculation
prev_price = df["price"].iloc[0]

for _, row in df.iterrows():
    price = row["price"]
    direction = row["direction"]
    date = row["date"]
    color = cmap(norm(row["buyer_capital_ratio"]))

    # Brick height based on actual price change
    if direction == 1:
        bottom = prev_price
        top = price
    else:
        top = prev_price
        bottom = price

    # Draw rectangle brick
    ax.bar(date, abs(top - bottom), bottom=bottom, width=0.8, color=color, edgecolor="black")

    prev_price = price

ax.set_title("Diagonal Renko Bricks Colored by Buyer Capital Ratio")
ax.set_ylabel("Price")
ax.grid(True, linestyle="--", alpha=0.5)

# Format x-axis as dates
ax.xaxis.set_major_locator(mdates.WeekdayLocator(interval=1))
ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y-%m-%d"))



# ---------------------------
# 2. Combined Buyer Ratios with Shaded Difference
# ---------------------------
axes[1].plot(df["date"], df["buyer_participation_ratio"], color="blue", label="Participation Ratio", linewidth=1)
axes[1].plot(df["date"], df["buyer_capital_ratio"], color="purple", label="Capital Ratio", linewidth=1)
axes[1].fill_between(df["date"], df["buyer_participation_ratio"], df["buyer_capital_ratio"],
                    where=(df["buyer_capital_ratio"] >= df["buyer_participation_ratio"]),
                    interpolate=True, alpha=0.3, color="green", label="Capital > Participation")
axes[1].fill_between(df["date"], df["buyer_participation_ratio"], df["buyer_capital_ratio"],
                    where=(df["buyer_capital_ratio"] < df["buyer_participation_ratio"]),
                    interpolate=True, alpha=0.3, color="red", label="Participation > Capital")
axes[1].axhline(0.5, color="gray", linestyle="--", alpha=0.7)
axes[1].set_ylabel("Ratio")
axes[1].legend(loc='upper left', fontsize='small')
axes[1].grid(True, linestyle="--", alpha=0.5)

# ---------------------------
# 3. Whale Impact
# ---------------------------
axes[2].bar(df["date"], df["whale_impact"], color="red", label="Whale Impact")
axes[2].axhline(0.5, color="gray", linestyle="--", alpha=0.7)
axes[2].set_ylabel("Whale Impact")
axes[2].legend()
axes[2].grid(True, linestyle="--", alpha=0.5)

# === Layout ===
plt.tight_layout()
plt.show()

# Example:
# python3 tick/plot_renko_old.py
