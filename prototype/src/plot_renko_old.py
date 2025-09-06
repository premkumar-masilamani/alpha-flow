import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates

# === Load Renko data ===
df = pd.read_csv("./BTCUSDT-2025-renko.csv", parse_dates=["date"])

# === Plot setup ===
fig, axes = plt.subplots(4, 1, figsize=(14, 12), sharex=True,
                         gridspec_kw={'height_ratios': [3, 1, 1, 1]})

# ---------------------------
# 1. Renko Bricks
# ---------------------------
ax = axes[0]
for i, row in df.iterrows():
    color = "green" if row["direction"] == 1 else "red"
    ax.bar(row["date"], 1, bottom=row["price"], color=color, edgecolor="black", width=0.8)

ax.set_title("Renko Bricks (VWAP-driven)")
ax.set_ylabel("Price")
ax.grid(True, linestyle="--", alpha=0.5)

# Format x-axis as dates
ax.xaxis.set_major_locator(mdates.WeekdayLocator(interval=1))
ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y-%m-%d"))

# ---------------------------
# 2. Buyer Participation Ratio
# ---------------------------
axes[1].plot(df["date"], df["buyer_participation_ratio"], color="blue", label="Participation Ratio")
axes[1].axhline(0.5, color="gray", linestyle="--", alpha=0.7)
axes[1].set_ylabel("Participation")
axes[1].legend()
axes[1].grid(True, linestyle="--", alpha=0.5)

# ---------------------------
# 3. Buyer Capital Ratio
# ---------------------------
axes[2].plot(df["date"], df["buyer_capital_ratio"], color="purple", label="Capital Ratio")
axes[2].axhline(0.5, color="gray", linestyle="--", alpha=0.7)
axes[2].set_ylabel("Capital")
axes[2].legend()
axes[2].grid(True, linestyle="--", alpha=0.5)

# ---------------------------
# 5. Whale Impact
# ---------------------------
axes[3].bar(df["date"], df["whale_impact"], color="red", label="Whale Impact")
axes[2].axhline(0.5, color="gray", linestyle="--", alpha=0.7)
axes[3].set_ylabel("Whale Impact")
axes[3].legend()
axes[3].grid(True, linestyle="--", alpha=0.5)

# === Layout ===
plt.tight_layout()
plt.show()

# python3 src/plot_renko_old.py
