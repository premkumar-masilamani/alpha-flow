package com.alphaflow.infrastructure.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtils {

    private NumberUtils() {
        // Private constructor to hide the implicit public one
    }

    public static BigDecimal convertToBigDecimal(Object value) {
        return switch (value) {
            case null -> null;
            case BigDecimal bigDecimal -> bigDecimal;
            case Double v -> BigDecimal.valueOf(v);
            case Integer i -> BigDecimal.valueOf(i);
            case Long l -> BigDecimal.valueOf(l);
            default -> new BigDecimal(value.toString());
        };
    }

    public static BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
