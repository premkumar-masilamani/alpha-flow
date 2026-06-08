package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceBarTest {

  @Test
  void testPriceBarAccessorsAndValueFor() {

    LocalDate date = LocalDate.of(2026, 5, 29);

    BigDecimal open = new BigDecimal("100.00");

    BigDecimal high = new BigDecimal("105.00");

    BigDecimal low = new BigDecimal("99.00");

    BigDecimal close = new BigDecimal("102.00");

    BigDecimal volume = new BigDecimal("1000");

    PriceBar bar = new PriceBar(date, open, high, low, close, volume);

    assertEquals(date, bar.date());

    assertEquals(open, bar.open());

    assertEquals(high, bar.high());

    assertEquals(low, bar.low());

    assertEquals(close, bar.close());

    assertEquals(volume, bar.volume());

    assertEquals(close, bar.valueFor(PriceSource.CLOSE));

    assertEquals(volume, bar.valueFor(PriceSource.VOLUME));
  }
}
