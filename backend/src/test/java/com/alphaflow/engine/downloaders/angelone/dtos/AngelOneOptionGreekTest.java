package com.alphaflow.engine.downloaders.angelone.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.engine.strategies.options.enums.OptionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AngelOneOptionGreekTest {

  @Test
  void testRecordFields() {
    BigDecimal strike = new BigDecimal("25000");
    BigDecimal delta = new BigDecimal("-0.5");
    BigDecimal gamma = new BigDecimal("0.001");
    BigDecimal theta = new BigDecimal("-10");
    BigDecimal vega = new BigDecimal("5");
    BigDecimal iv = new BigDecimal("15");
    BigDecimal volume = new BigDecimal("1000");

    AngelOneOptionGreek greek =
        new AngelOneOptionGreek(
            strike, OptionType.PUT, delta, gamma, theta, vega, iv, volume);

    assertEquals(strike, greek.strikePrice());
    assertEquals(OptionType.PUT, greek.optionType());
    assertEquals(delta, greek.delta());
    assertEquals(gamma, greek.gamma());
    assertEquals(theta, greek.theta());
    assertEquals(vega, greek.vega());
    assertEquals(iv, greek.impliedVolatility());
    assertEquals(volume, greek.tradeVolume());
  }
}
