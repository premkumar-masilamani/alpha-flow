package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.engine.indicators.RSIIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RSIIndicatorTest {

  @Test
  void rsiAllGainsIsHundred() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new RSIIndicator()
            .compute(
                closes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.values()
        .forEach(
            m -> assertEquals(0, m.get("value").compareTo(bd(100)), "all-gains RSI must be 100"));
  }

  @Test
  void rsiAllLossesIsZero() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new RSIIndicator()
            .compute(
                closes(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.values()
        .forEach(m -> assertEquals(0, m.get("value").compareTo(bd(0)), "all-losses RSI must be 0"));
  }

  @Test
  void rsiStaysInRange() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new RSIIndicator()
            .compute(walk(100), IndicatorParams.parse("period=14"), PriceSource.CLOSE);

    r.values()
        .forEach(
            m -> {
              BigDecimal val = m.get("value");
              assertTrue(val.compareTo(BigDecimal.ZERO) >= 0);

              assertTrue(val.compareTo(IndicatorMath.HUNDRED) <= 0);
            });
  }

  @Test
  void rsiFlatPricesOutputHundred() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new RSIIndicator()
            .compute(
                closes(
                    10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10),
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.values()
        .forEach(
            m -> assertEquals(0, m.get("value").compareTo(bd(100)), "flat prices RSI must be 100"));
  }
}
