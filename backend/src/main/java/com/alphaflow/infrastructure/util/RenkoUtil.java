package com.alphaflow.infrastructure.util;

public class RenkoUtil {

    public static int getZoneFromTrend(int trend) {
        if (trend <= 3) return 1;
        if (trend <= 9) return 2;
        if (trend <= 27) return 3;
        if (trend <= 81) return 4;
        return 5;
    }
}
