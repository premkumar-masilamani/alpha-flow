package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.WeeklyPrice;
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

  @Test
  void testFromDailyPrice() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    DailyPrice dailyPrice =
        DailyPrice.builder()
            .priceDate(date)
            .priceOpen(new BigDecimal("100.00"))
            .priceHigh(new BigDecimal("105.00"))
            .priceLow(new BigDecimal("99.00"))
            .priceClose(new BigDecimal("102.00"))
            .volume(new BigDecimal("1000"))
            .build();

    PriceBar bar = PriceBar.from(dailyPrice);

    assertEquals(date, bar.date());
    assertEquals(new BigDecimal("100.00"), bar.open());
    assertEquals(new BigDecimal("105.00"), bar.high());
    assertEquals(new BigDecimal("99.00"), bar.low());
    assertEquals(new BigDecimal("102.00"), bar.close());
    assertEquals(new BigDecimal("1000"), bar.volume());
  }

  @Test
  void testFromWeeklyPrice() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    WeeklyPrice weeklyPrice =
        WeeklyPrice.builder()
            .priceDate(date)
            .priceOpen(new BigDecimal("98.00"))
            .priceHigh(new BigDecimal("106.00"))
            .priceLow(new BigDecimal("97.00"))
            .priceClose(new BigDecimal("104.00"))
            .volume(new BigDecimal("5000"))
            .build();

    PriceBar bar = PriceBar.from(weeklyPrice);

    assertEquals(date, bar.date());
    assertEquals(new BigDecimal("98.00"), bar.open());
    assertEquals(new BigDecimal("106.00"), bar.high());
    assertEquals(new BigDecimal("97.00"), bar.low());
    assertEquals(new BigDecimal("104.00"), bar.close());
    assertEquals(new BigDecimal("5000"), bar.volume());
  }
}
