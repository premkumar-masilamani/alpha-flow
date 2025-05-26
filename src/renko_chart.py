import logging
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import matplotlib.ticker as mticker
from helpers import load_timeseries_data, get_renko_file_path, parse_chart_args

logger = logging.getLogger(__name__)

CHART_WIDTH_INCHES = 16.0
CHART_HEIGHT_INCHES = 8.0
CHART_BRICKS_COUNT = 180  # 6 Months Data
SHOW_TREND_NUMBER = True  # Display the trend number inside the bricks
TREND_FONT_SIZE = 5


def plot_renko(df: pd.DataFrame, ticker: str, timeframe: str):
    logger.info(f"Plotting Renko chart for {ticker} ({timeframe})")
    fig, ax = plt.subplots()
    fig.set_size_inches(CHART_WIDTH_INCHES, CHART_HEIGHT_INCHES)
    ax.set_facecolor("#EEEEEE")  # Light gray background
    ax.set_title(f"Renko Chart - {ticker} ({timeframe})")
    ax.set_xlabel("Date")
    ax.set_ylabel("Price in USD")
    ax.yaxis.tick_right()
    ax.yaxis.set_label_position("right")
    ax.yaxis.set_major_formatter(mticker.StrMethodFormatter("${x:,.2f}"))
    ax.grid(True)
    ax.format_coord = lambda x, y: format_coord(df, brick_size, x, y)

    x = 0  # X-coordinate for bricks
    first_row = df.iloc[0]
    brick_size = first_row["brick_high"] - first_row["brick_low"]

    for i, row in df.iterrows():
        color = "green" if row["direction"] == "up" else "red"
        trend = row["trend"]
        y_low = row["brick_low"]

        rect = patches.Rectangle(
            (float(x), float(y_low)),
            width=brick_size,
            height=brick_size,
            color=color,
        )
        ax.add_patch(rect)

        # Add trend text
        text_x = x + brick_size / 2
        text_y = y_low + brick_size / 2
        text_color = "black" if SHOW_TREND_NUMBER else color
        ax.text(
            text_x,
            text_y,
            f"{trend}",
            ha="center",
            va="center",
            color=text_color,
            fontsize=TREND_FONT_SIZE,
        )
        logger.debug(
            f"Brick {i}: x={x}, y_low={y_low}, height={brick_size}, color={color}, trend={trend}"
        )

        # Move next brick by brick size
        x += brick_size

    x_padding = brick_size * 10
    y_padding = df["brick_high"].max() * 0.10
    ax.set_xlim(0, len(df) * brick_size + x_padding)
    ax.set_ylim(df["brick_low"].min() - y_padding, df["brick_high"].max() + y_padding)

    plt.tight_layout()
    plt.show()


def format_coord(df, brick_size, x_hover, y_hover):
    logger.debug(f"Formatting coordinates for x={x_hover}, y={y_hover}")
    i = int(x_hover // brick_size)
    if 0 <= i < len(df):
        row = df.iloc[i]
        date = pd.to_datetime(row["date"]).strftime("%d-%b-%Y")
        price = f"${row['brick_high']:,.2f} - ${row['brick_low']:,.2f}"
        return f"Date: {date}, Price: {price}"
    else:
        return f"x={x_hover:.2f}, y={y_hover:.2f}"


if __name__ == "__main__":
    """Main entry point for the program."""
    args = parse_chart_args()
    data_dir = args.data_dir.lower()
    ticker = args.ticker.upper()
    timeframe = args.timeframe.lower()

    renko_df = load_timeseries_data(get_renko_file_path(data_dir, ticker, timeframe))

    if len(renko_df) > CHART_BRICKS_COUNT:
        renko_df = renko_df.iloc[-CHART_BRICKS_COUNT:]

    plot_renko(renko_df, ticker, timeframe)
