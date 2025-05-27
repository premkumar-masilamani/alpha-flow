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
COLOR_CHART_BACKGROUND: str = "#EEEEEE" # Light Gray

SHOW_TREND_NUMBER: bool = True  # Display the trend number inside the bricks
BRICK_TEXT_FONT_SIZE: int = 5
COLOR_BRICK_TEXT: str = "black"

COLOR_CURRENT_PRICE: str = "blue"
COLOR_SL_PRICE: str = "red"

COLOR_GMMA_SHORT_EMA: str = "green"
COLOR_GMMA_LONG_EMA: str = "red"


def plot_renko(renko_df: pd.DataFrame, renko_ma_df: pd.DataFrame, ticker: str, timeframe: str):
    logger.info(f"Plotting Renko chart for {ticker} ({timeframe})")

    # #################
    # Base Renko Chart
    # #################

    fig, ax = plt.subplots()
    fig.set_size_inches(CHART_WIDTH_INCHES, CHART_HEIGHT_INCHES)
    ax.set_facecolor(COLOR_CHART_BACKGROUND)
    ax.set_title(f"Renko Chart - {ticker} ({timeframe})")
    ax.set_xlabel("Date")
    ax.set_ylabel("Price in USD")
    ax.yaxis.tick_right()
    ax.yaxis.set_label_position("right")
    ax.yaxis.set_major_formatter(mticker.StrMethodFormatter("${x:,.2f}"))
    ax.grid(True)
    ax.format_coord = lambda x, y: format_coord(renko_df, brick_size, x, y)

    date_to_x = {}
    x = 0  # X-coordinate for bricks
    first_row = renko_df.iloc[0]
    brick_size = first_row["brick_high"] - first_row["brick_low"]

    for i, row in renko_df.iterrows():
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
        text_color = COLOR_BRICK_TEXT if SHOW_TREND_NUMBER else color
        ax.text(
            text_x,
            text_y,
            f"{trend}",
            ha="center",
            va="center",
            color=text_color,
            fontsize=BRICK_TEXT_FONT_SIZE,
        )
        logger.debug(
            f"Brick {i}: x={x}, y_low={y_low}, height={brick_size}, color={color}, trend={trend}"
        )
        # Used for plotting GMMA
        date_to_x[row['date']] = x
        # Move next brick by brick size
        x += brick_size

    # ###################
    # Current / SL Lines
    # ###################
    current_price, sl_price = get_prices(renko_df)
    ax.axhline(
        y=current_price, color=COLOR_CURRENT_PRICE, linestyle="-", linewidth=0.5
    )
    ax.text(
        0.5,
        current_price,
        f" Current - ${current_price:,.2f}",
        ha="left",
        va="bottom",
        color=COLOR_CURRENT_PRICE,
    )
    ax.axhline(y=sl_price, color=COLOR_SL_PRICE, linestyle="-", linewidth=0.5)
    ax.text(
        0.5,
        sl_price,
        f" SL - ${sl_price:,.2f}",
        ha="left",
        va="bottom",
        color=COLOR_SL_PRICE,
    )

    # ###############
    # GMMA Indicator
    # ###############

    short_emas = [3, 5, 8, 10, 12, 15]
    for ema in short_emas:
        x_coords = []
        y_coords = []
        for date in renko_ma_df['date']:
            if date in date_to_x:
                x = date_to_x[date] + brick_size / 2
                try:
                    y = renko_ma_df.loc[renko_ma_df['date'] == date, f'ema_{ema}'].iloc[0]
                    if not pd.isna(y):
                        x_coords.append(x)
                        y_coords.append(y)
                except (KeyError, IndexError) as e:
                    logger.debug(f"Skipping {date} for ema_{ema} due to error: {e}")
                    continue
        ax.plot(x_coords, y_coords, color=COLOR_GMMA_SHORT_EMA, linewidth=0.5)

    long_emas = [30, 35, 40, 45, 50, 60]
    for ema in long_emas:
        x_coords = []
        y_coords = []
        for date in renko_ma_df['date']:
            if date in date_to_x:
                x = date_to_x[date] + brick_size / 2
                try:
                    y = renko_ma_df.loc[renko_ma_df['date'] == date, f'ema_{ema}'].iloc[0]
                    if not pd.isna(y):
                        x_coords.append(x)
                        y_coords.append(y)
                except (KeyError, IndexError) as e:
                    logger.debug(f"Skipping {date} for ema_{ema} due to error: {e}")
                    continue
        ax.plot(x_coords, y_coords, color=COLOR_GMMA_LONG_EMA, linewidth=0.5)

    # #################
    # Base Renko Chart
    # #################

    x_padding = brick_size * 10
    y_padding = renko_df["brick_high"].max() * 0.10
    ax.set_xlim(0, len(renko_df) * brick_size + x_padding)
    ax.set_ylim(renko_df["brick_low"].min() - y_padding, renko_df["brick_high"].max() + y_padding)
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

    # Plot Renko Chart wth GMMA Indicator
    plot_renko(renko_df, renko_ma_df, ticker, timeframe)
