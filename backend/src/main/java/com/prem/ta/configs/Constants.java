package com.prem.ta.configs;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class Constants {

    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String getBinanceZipFileName(String tickerSymbol, String dateStr) {
        return tickerSymbol + "-trades-" + dateStr + ".zip";
    }

    public static String getBinanceFormattedDateString(TemporalAccessor date) {
        return dateFormatter.format(date);
    }
}
