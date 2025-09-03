import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors

# Load Renko CSV
df = pd.read_csv("./BTCUSDT-2025-renko.csv", parse_dates=["date"])

# Color mapping for buyer_capital_ratio
norm = mcolors.Normalize(vmin=df["buyer_capital_ratio"].min(),
                         vmax=df["buyer_capital_ratio"].max())
cmap = plt.cm.RdYlGn

# Initialize previous price for brick height calculation
prev_price = df["price"].iloc[0]
x_seq = 0

fig, ax = plt.subplots(figsize=(14, 6))

for _, row in df.iterrows():
    price = row["price"]
    direction = row["direction"]
    color = cmap(norm(row["buyer_capital_ratio"]))

    # Brick height based on actual price change
    if direction == 1:
        bottom = prev_price
        top = price
    else:
        top = prev_price
        bottom = price

    # Draw rectangle brick
    ax.bar(x_seq, abs(top - bottom), bottom=bottom, width=0.9, color=color, edgecolor="black")

    prev_price = price
    x_seq += 1

ax.set_title("Diagonal Renko Bricks Colored by Buyer Capital Ratio")
ax.set_xlabel("Brick Sequence")
ax.set_ylabel("Price")
ax.grid(True, linestyle="--", alpha=0.5)

# Colorbar
sm = plt.cm.ScalarMappable(cmap=cmap, norm=norm)
sm.set_array([])
cbar = plt.colorbar(sm, ax=ax)
cbar.set_label("Buyer Capital Ratio")

plt.tight_layout()
plt.show()
