package com.alphaflow.engine.enums;

public enum WindowPeriod {

    ZERO_DAYS(0),
    THREE_DAYS(3),
    FOUR_DAYS(4),
    FIVE_DAYS(5),
    SIX_DAYS(6),
    SEVEN_DAYS(7),
    EIGHT_DAYS(8),
    NINE_DAYS(9),
    TEN_DAYS(10),
    ELEVEN_DAYS(11),
    TWELVE_DAYS(12),
    THIRTEEN_DAYS(13),
    FOURTEEN_DAYS(14),
    FIFTEEN_DAYS(15),
    SIXTEEN_DAYS(16),
    SEVENTEEN_DAYS(17),
    EIGHTEEN_DAYS(18),
    NINETEEN_DAYS(19),
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

    public static WindowPeriod fromDays(int days) {
        for (WindowPeriod period : WindowPeriod.values()) {
            if (period.days == days) {
                return period;
            }
        }
        throw new IllegalArgumentException("No WindowPeriod found for " + days + " days");
    }
}
