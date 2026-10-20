package com.alphaflow.engine.downloaders.angelone.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AngelOneInstrumentTest {

  @Test
  void testRecordFields() {
    LocalDate expiry = LocalDate.of(2026, 11, 9);
    BigDecimal strike = new BigDecimal("25000");

    AngelOneInstrument instrument =
        new AngelOneInstrument(
            "12345",
            "NIFTY09NOV2625000PE",
            "NIFTY",
            expiry,
            strike,
            65,
            "OPTIDX",
            "NFO");

    assertEquals("12345", instrument.token());
    assertEquals("NIFTY09NOV2625000PE", instrument.symbol());
    assertEquals("NIFTY", instrument.name());
    assertEquals(expiry, instrument.expiry());
    assertEquals(strike, instrument.strike());
    assertEquals(65, instrument.lotSize());
    assertEquals("OPTIDX", instrument.instrumentType());
    assertEquals("NFO", instrument.exchange());
  }
}
