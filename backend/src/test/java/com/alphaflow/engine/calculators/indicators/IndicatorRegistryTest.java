package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.persistence.enums.IndicatorType;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndicatorRegistryTest {

  @Test
  void testRegistryResolvesKnownRejectsDuplicateAndUnknown() {

    SmaIndicator sma = new SmaIndicator();

    EmaIndicator ema = new EmaIndicator();

    IndicatorRegistry registry = new IndicatorRegistry(List.of(sma, ema));

    assertEquals(sma, registry.get(IndicatorType.SMA));

    assertEquals(ema, registry.get(IndicatorType.EMA));

    // Rejects duplicate beans

    assertThrows(IllegalStateException.class, () -> new IndicatorRegistry(List.of(sma, sma)));

    // Rejects unregistered/unknown type

    assertThrows(IllegalArgumentException.class, () -> registry.get(IndicatorType.RSI));
  }
}
