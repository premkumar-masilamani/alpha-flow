import logging
from ntpath import curdir
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import matplotlib.ticker as mticker
from helpers import load_timeseries_data, get_renko_file_path,get_renko_ma_file_path, parse_chart_args, get_zone_from_trend

logger = logging.getLogger(__name__)

CHART_WIDTH_INCHES: float = 16.0
CHART_HEIGHT_INCHES: float = 8.0
CHART_BRICKS_COUNT: int = 180  # 6 Months Data
SHOW_TREND_NUMBER: bool = True  # Display the trend number inside the bricks
TREND_FONT_SIZE: int = 5
CURRENT_PRICE_LINE_COLOR: str = "blue"
SL_PRICE_LINE_COLOR: str = "red"


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

    # Current Brick and SL Brick
    current_price, sl_price = get_prices(df)
    ax.axhline(
        y=current_price, color=CURRENT_PRICE_LINE_COLOR, linestyle="-", linewidth=0.5
    )
    ax.text(
        0.5,
        current_price,
        f" Current - ${current_price:,.2f}",
        ha="left",
        va="bottom",
        color=CURRENT_PRICE_LINE_COLOR,
    )
    ax.axhline(y=sl_price, color=SL_PRICE_LINE_COLOR, linestyle="-", linewidth=0.5)
    ax.text(
        0.5,
        sl_price,
        f" SL - ${sl_price:,.2f}",
        ha="left",
        va="bottom",
        color=SL_PRICE_LINE_COLOR,
    )

    plt.tight_layout()
    plt.show()


def format_coord(df, brick_size, x_hover, y_hover):
    logger.debug(f"Formatting coordinates for x={x_hover}, y={y_hover}")
    i = int(x_hover // brick_size)
    if 0 <= i < len(df):
        row = df.iloc[i]
        date = pd.to_datetime(row["date"]).strftime("%d-%b-%Y")
        price = f"${row['brick_low']:,.2f} - ${row['brick_high']:,.2f}"
        return f"Date: {date}, Price: {price}"
    else:
        return f"x={x_hover:.2f}, y={y_hover:.2f}"


def get_prices(df: pd.DataFrame) -> tuple[float, float]:
    """
    Calculates the current price and stop loss (SL) price based on Renko chart trends.

    Args:
        df (pd.DataFrame): The Renko chart DataFrame.

    Returns:
        tuple[float, float]: A tuple containing the current price and SL price.
    """
    current_price = 0.0
    sl_price = 0.0
    current_brick = None
    sl_brick = None

    # Get latest brick
    latest_brick = df.iloc[-1]
    latest_direction = latest_brick['direction']

    # Get latest brick with opposite direction
    opposite_direction = 'down' if latest_direction == 'up' else 'up'
    latest_opposite_brick = df[df['direction'] == opposite_direction].iloc[-1]

    # Find the current trend by checking if the latest brick's trend is greater than
    # the opposite brick's zone (i.e.) a new trend has started
    latest_brick_trend = latest_brick['trend']
    latest_opposite_brick_zone = get_zone_from_trend(latest_opposite_brick['trend'])
    if latest_brick_trend > (latest_opposite_brick_zone + 1):
        current_brick = latest_brick
    else:
        current_brick = latest_opposite_brick

    current_idx = df.index.get_loc(current_brick.name)
    current_brick_zone = get_zone_from_trend(current_brick['trend'])
    sl_brick = df.iloc[current_idx - (current_brick_zone + 1)]

    if current_brick['direction'] == 'up':
        current_price = current_brick['brick_high']
        sl_price = sl_brick['brick_low']
    else:
        current_price = current_brick['brick_low']
        sl_price = sl_brick['brick_high']

    return current_price, sl_price


if __name__ == "__main__":
    """Main entry point for the program."""
    args = parse_chart_args()
    data_dir = args.data_dir.lower()
    ticker = args.ticker.upper()
    timeframe = args.timeframe.lower()

    renko_df = load_timeseries_data(get_renko_file_path(data_dir, ticker, timeframe))
    renko_ma_df = load_timeseries_data(get_renko_ma_file_path(data_dir, ticker, timeframe))

    if len(renko_df) > CHART_BRICKS_COUNT:
        renko_df = renko_df.iloc[-CHART_BRICKS_COUNT:]

    # Get current price and SL price
    current_price, sl_price = get_prices(renko_df)
    logger.info(f"Calculated Current Price: {current_price}, SL Price: {sl_price}")

    # TODO: Plot Renko chart with GMMA using the specific EMA values
    # Short term EMAs, plotted in green color with line width 1 [3, 5, 8, 10, 12, 15]
    # Long term EMAs, plotted in red color with line width 1 [30, 35, 40, 45, 50, 60]
    plot_renko_with_ma(renko_df, renko_ma_df, ticker, timeframe)
