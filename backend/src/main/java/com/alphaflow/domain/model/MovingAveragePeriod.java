package com.alphaflow.domain.model;

public enum MovingAveragePeriod {

    ONE_WEEK(7),
    TWO_WEEKS(14),
    ONE_MONTH(30),
    TWO_MONTHS(60),
    THREE_MONTHS(90);

    private final int days;

    MovingAveragePeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
