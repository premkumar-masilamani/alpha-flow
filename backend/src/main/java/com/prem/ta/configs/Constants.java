package com.prem.ta.configs;

import lombok.Getter;

import java.time.format.DateTimeFormatter;

public class Constants {

    @Getter
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

}
