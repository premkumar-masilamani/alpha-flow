package com.alphaflow.engine.enums;

public enum WindowPeriod {

    ZERO_DAYS(0),
    FIVE_DAYS(5),
    TEN_DAYS(10),
    TWENTY_DAYS(20),
    TWENTY_ONE_DAYS(21);

    private final int days;

    WindowPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
