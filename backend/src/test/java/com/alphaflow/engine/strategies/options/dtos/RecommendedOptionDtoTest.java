package com.alphaflow.engine.strategies.options.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.engine.strategies.options.enums.OptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecommendedOptionDtoTest {

  @Test
  void testRecordFields() {
    LocalDate expiry = LocalDate.of(2026, 11, 9);
    BigDecimal strike = new BigDecimal("25000");
    BigDecimal ltp = new BigDecimal("650");
    BigDecimal iv = new BigDecimal("500");
    BigDecimal tv = new BigDecimal("150");
    BigDecimal delta = new BigDecimal("-0.6");
    BigDecimal impliedVol = new BigDecimal("14.5");
    BigDecimal theta = new BigDecimal("-8.2");

    RecommendedOptionDto dto =
        new RecommendedOptionDto(
            "NIFTY09NOV2625000PE",
            "12345",
            strike,
            OptionType.PUT,
            expiry,
            12L,
            ltp,
            iv,
            tv,
            delta,
            impliedVol,
            theta);

    assertEquals("NIFTY09NOV2625000PE", dto.tradingSymbol());
    assertEquals("12345", dto.token());
    assertEquals(strike, dto.strikePrice());
    assertEquals(OptionType.PUT, dto.optionType());
    assertEquals(expiry, dto.expiryDate());
    assertEquals(12L, dto.daysToExpiry());
    assertEquals(ltp, dto.lastPrice());
    assertEquals(iv, dto.intrinsicValue());
    assertEquals(tv, dto.timeValue());
    assertEquals(delta, dto.delta());
    assertEquals(impliedVol, dto.impliedVolatility());
    assertEquals(theta, dto.theta());
  }
}
