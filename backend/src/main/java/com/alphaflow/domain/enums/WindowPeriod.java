package com.alphaflow.domain.enums;

public enum WindowPeriod {

    FIVE_DAYS(5),
    TEN_DAYS(10),
    TWENTY_DAYS(20),
    TWENTY_ONE_DAYS(21),
    ZERO(0);

    private final int days;

    WindowPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
