package com.prem.ta.configs;

import java.time.format.DateTimeFormatter;

public class Utils {

    public static final String DOWNLOAD_SOURCE = "Binance";
    public static final String CHECKSUM_ALGORITHM = "SHA-256";
    public static final int PAGE_SIZE = 10;
    public static final String INTERVAL_DAILY = "1d";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd"
    );

    public static DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }
}
