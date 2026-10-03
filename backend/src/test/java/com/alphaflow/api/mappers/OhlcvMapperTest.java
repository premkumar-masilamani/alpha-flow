package com.alphaflow.api.mappers;

import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OhlcvMapperTest {

  @Test
  void testDailyPriceMappingWithIndianTicker() {
    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").country(Country.IN).build();
    LocalDate date = LocalDate.of(2026, 9, 23);
    DailyPrice dailyPrice =
        DailyPrice.builder()
            .ticker(ticker)
            .priceDate(date)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25100.0000"))
            .priceLow(new BigDecimal("24950.0000"))
            .priceClose(new BigDecimal("25050.0000"))
            .volume(new BigDecimal("500000.0000"))
            .build();

    OhlcvDto dto = OhlcvMapper.toDto(dailyPrice);

    assertNotNull(dto);
    assertEquals(date.atStartOfDay(MarketConstants.IST_ZONE).toOffsetDateTime(), dto.priceDate());
    assertEquals(ZoneOffset.ofHoursMinutes(5, 30), dto.priceDate().getOffset());
    assertEquals(new BigDecimal("25000.0000"), dto.priceOpen());
    assertEquals(new BigDecimal("25100.0000"), dto.priceHigh());
    assertEquals(new BigDecimal("24950.0000"), dto.priceLow());
    assertEquals(new BigDecimal("25050.0000"), dto.priceClose());
    assertEquals(new BigDecimal("500000.0000"), dto.volume());
  }

  @Test
  void testDailyPriceMappingWithUsTicker() {
    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").country(Country.US).build();
    LocalDate date = LocalDate.of(2026, 5, 29);
    DailyPrice dailyPrice =
        DailyPrice.builder()
            .ticker(ticker)
            .priceDate(date)
            .priceOpen(new BigDecimal("180.0000"))
            .priceHigh(new BigDecimal("185.0000"))
            .priceLow(new BigDecimal("179.0000"))
            .priceClose(new BigDecimal("182.0000"))
            .volume(new BigDecimal("1000000.0000"))
            .build();

    OhlcvDto dto = OhlcvMapper.toDto(dailyPrice);

    assertNotNull(dto);
    assertEquals(date.atStartOfDay(MarketConstants.EST_ZONE).toOffsetDateTime(), dto.priceDate());
    assertEquals(
        date.atStartOfDay(MarketConstants.EST_ZONE).getOffset(), dto.priceDate().getOffset());
    assertEquals(new BigDecimal("180.0000"), dto.priceOpen());
  }

  @Test
  void testDailyPriceMappingWithDefaultUtcTicker() {
    Ticker ticker = Ticker.builder().tickerSymbol("GLOBAL").country(null).build();
    LocalDate date = LocalDate.of(2026, 1, 15);
    DailyPrice dailyPrice =
        DailyPrice.builder()
            .ticker(ticker)
            .priceDate(date)
            .priceOpen(new BigDecimal("100.0000"))
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("99.0000"))
            .priceClose(new BigDecimal("102.0000"))
            .volume(new BigDecimal("10000.0000"))
            .build();

    OhlcvDto dto = OhlcvMapper.toDto(dailyPrice);

    assertNotNull(dto);
    assertEquals(date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(), dto.priceDate());
    assertEquals(ZoneOffset.UTC, dto.priceDate().getOffset());
  }

  @Test
  void testWeeklyPriceMapping() {
    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").country(Country.US).build();
    LocalDate date = LocalDate.of(2026, 5, 29);
    WeeklyPrice weeklyPrice =
        WeeklyPrice.builder()
            .ticker(ticker)
            .priceDate(date)
            .priceOpen(new BigDecimal("175.0000"))
            .priceHigh(new BigDecimal("185.0000"))
            .priceLow(new BigDecimal("174.0000"))
            .priceClose(new BigDecimal("182.0000"))
            .volume(new BigDecimal("5000000.0000"))
            .build();

    OhlcvDto dto = OhlcvMapper.toDto(weeklyPrice);

    assertNotNull(dto);
    assertEquals(date.atStartOfDay(MarketConstants.EST_ZONE).toOffsetDateTime(), dto.priceDate());
  }

  @Test
  void testIntradayPriceMapping() {
    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").country(Country.IN).build();
    OffsetDateTime time =
        LocalDate.of(2026, 9, 23).atTime(9, 15).atZone(MarketConstants.IST_ZONE).toOffsetDateTime();
    IntradayPrice intradayPrice =
        IntradayPrice.builder()
            .ticker(ticker)
            .priceTime(time)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25050.0000"))
            .priceLow(new BigDecimal("24990.0000"))
            .priceClose(new BigDecimal("25040.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    OhlcvDto dto = OhlcvMapper.toDto(intradayPrice);

    assertNotNull(dto);
    assertEquals(time, dto.priceDate());
  }
}
