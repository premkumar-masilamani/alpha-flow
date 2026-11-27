package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IndicatorTestHelper {

  public static final LocalDate EPOCH = LocalDate.of(2020, 1, 1);

  private IndicatorTestHelper() {}

  public static List<PriceBar> walk(int n) {

    List<PriceBar> bars = new ArrayList<>(n);

    for (int i = 0; i < n; i++) {

      double close = 100 + 12 * Math.sin(i / 5.0) + 0.3 * i + 4 * Math.sin(i / 1.7);

      bars.add(barAt(i, close, close + 2, close - 2, close));
    }

    return bars;
  }

  public static List<PriceBar> closes(double... closes) {

    List<PriceBar> bars = new ArrayList<>(closes.length);

    for (int i = 0; i < closes.length; i++) {

      bars.add(barAt(i, closes[i], closes[i], closes[i], closes[i]));
    }

    return bars;
  }

  public static PriceBar barAt(int dayOffset, double open, double high, double low, double close) {

    return new PriceBar(
        EPOCH.plusDays(dayOffset),
        bd(open),
        bd(high),
        bd(low),
        bd(close),
        BigDecimal.valueOf(1000L + dayOffset));
  }

  public static BigDecimal bd(double v) {

    return BigDecimal.valueOf(v).setScale(4, IndicatorMath.ROUNDING);
  }

  public static Map<LocalDate, Map<String, BigDecimal>> from(
      Map<LocalDate, Map<String, BigDecimal>> values, LocalDate fromInclusive) {
    Map<LocalDate, Map<String, BigDecimal>> result = new java.util.LinkedHashMap<>();
    values.forEach(
        (date, map) -> {
          if (!date.isBefore(fromInclusive)) {
            result.put(date, map);
          }
        });
    return result;
  }

  public static BigDecimal plot(
      Map<LocalDate, Map<String, BigDecimal>> values, LocalDate date, String output) {
    Map<String, BigDecimal> map = values.get(date);
    if (map == null) {
      throw new IllegalArgumentException("No indicator values found for date: " + date);
    }
    BigDecimal value = map.get(output);
    if (value == null) {
      throw new IllegalArgumentException("No plot point found for " + date + " " + output);
    }
    return value;
  }
}
