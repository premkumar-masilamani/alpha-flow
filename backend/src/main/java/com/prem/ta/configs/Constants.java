package com.prem.ta.configs;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class Constants {

    public static final int BINANCE_TICK_DATA_COLUMN_PRICE = 1;
    public static final int BINANCE_TICK_DATA_COLUMN_QUANTITY = 2;
    public static final int BINANCE_TICK_DATA_COLUMN_QUOTE_QUANTITY = 3;
    public static final int BINANCE_TICK_DATA_COLUMN_IS_BUYER_THE_MAKER = 5;

    public static final double VOLUME_PROFILE_RANGE_BIN_PERCENT = 0.05;
    public static final double VOLUME_PROFILE_VALUE_AREA_PERCENT = 0.70;

    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String getBinanceZipFileName(String tickerSymbol, String dateStr) {
        return tickerSymbol + "-trades-" + dateStr + ".zip";
    }

    public static String getBinanceFormattedDateString(TemporalAccessor date) {
        return dateFormatter.format(date);
    }
}
