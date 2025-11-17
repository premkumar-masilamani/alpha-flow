package com.prem.ta.configs;

import lombok.Getter;

import java.time.format.DateTimeFormatter;

public class Constants {

    public static final String CHECKSUM_ALGORITHM = "SHA-256";
    public static final int PAGE_SIZE = 10;

    @Getter
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

}
