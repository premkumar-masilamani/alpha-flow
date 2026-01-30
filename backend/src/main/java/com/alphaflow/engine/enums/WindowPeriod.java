package com.alphaflow.engine.enums;

public enum WindowPeriod {

    ZERO_DAYS(0),
    THREE_DAYS(3),
    FIVE_DAYS(5),
    EIGHT_DAYS(8),
    TEN_DAYS(10),
    TWELVE_DAYS(12),
    FIFTEEN_DAYS(15),
    TWENTY_DAYS(20),
    TWENTY_ONE_DAYS(21),
    THIRTY_DAYS(30),
    THIRTY_FIVE_DAYS(35),
    FORTY_DAYS(40),
    FORTY_FIVE_DAYS(45),
    FIFTY_DAYS(50),
    SIXTY_DAYS(60),
    TWO_HUNDRED_DAYS(200);

    private final int days;

    WindowPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
