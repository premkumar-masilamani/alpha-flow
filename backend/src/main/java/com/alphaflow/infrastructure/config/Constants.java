package com.alphaflow.infrastructure.config;

import tech.tablesaw.api.ColumnType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.temporal.TemporalAccessor;

import static java.time.format.DateTimeFormatter.ofPattern;

public class Constants {

    public static final ColumnType[] BINANCE_TICK_DATA_SCHEMA = {
            ColumnType.LONG,    // trade id (integer, safe as LONG)
            ColumnType.STRING,  // price (parsed as STRING to preserve precision)
            ColumnType.STRING,  // quantity (parsed as STRING to preserve precision)
            ColumnType.STRING,  // quote quantity (parsed as STRING to preserve precision)
            ColumnType.LONG,    // trade time (integer, safe as LONG)
            ColumnType.BOOLEAN, // is buyer maker
            ColumnType.BOOLEAN  // is best match
    };
    public static final int BINANCE_TICK_DATA_COLUMN_INDEX_PRICE = 1;
    public static final int BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY = 2;
    public static final int BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY = 3;
    public static final int BINANCE_TICK_DATA_COLUMN_INDEX_IS_BUYER_THE_MAKER = 5;

    public static final double CAPITAL_PROFILE_RANGE_BIN_PERCENT = 0.05;
    public static final double CAPITAL_PROFILE_VALUE_AREA_PERCENT = 0.70;

    public static final MathContext DB_MATH_CONTEXT = new MathContext(28);
    public static final int DB_QUERY_PAGE_SIZE = 10;

    public static final int SCALE = 8;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static final int RENKO_BRICK_SIZE_PERIOD = 9;
    public static final String RENKO_BRICK_DIRECTION_UP = "up";
    public static final String RENKO_BRICK_DIRECTION_DOWN = "down";

    public static String getBinanceZipFileName(String tickerSymbol, String dateStr) {
        return tickerSymbol + "-trades-" + dateStr + ".zip";
    }

    public static String getBinanceDateString(TemporalAccessor date) {
        return ofPattern("yyyy-MM-dd").format(date);
    }
}
