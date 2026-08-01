package com.alphaflow.api.mappers;

import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IndicatorMapperTest {

  @Test
  void testLabelAllTypesAndSources() {

    // EMA Close

    assertEquals(
        "EMA (20)",
        IndicatorMapper.label(
            IndicatorType.EMA, PriceSource.CLOSE, IndicatorParams.of(Map.of("period", 20))));

    // SMA
    assertEquals(
        "SMA (50)",
        IndicatorMapper.label(
            IndicatorType.SMA, PriceSource.CLOSE, IndicatorParams.of(Map.of("period", 50))));

    // SMA Volume (Vol (20))
    assertEquals(
        "Vol (20)",
        IndicatorMapper.label(
            IndicatorType.SMA, PriceSource.VOLUME, IndicatorParams.of(Map.of("period", 20))));

    // RSI

    assertEquals(
        "RSI (14)",
        IndicatorMapper.label(
            IndicatorType.RSI, PriceSource.CLOSE, IndicatorParams.of(Map.of("period", 14))));

    // MACD (with default signal)

    assertEquals(
        "MACD (12,26,9)",
        IndicatorMapper.label(
            IndicatorType.MACD,
            PriceSource.CLOSE,
            IndicatorParams.of(Map.of("fast", 12, "slow", 26))));

    // MACD (with custom signal)

    assertEquals(
        "MACD (12,26,8)",
        IndicatorMapper.label(
            IndicatorType.MACD,
            PriceSource.CLOSE,
            IndicatorParams.of(Map.of("fast", 12, "slow", 26, "signal", 8))));

    // Stochastic

    assertEquals(
        "Stoch (14,3,3)",
        IndicatorMapper.label(
            IndicatorType.STOCHASTIC,
            PriceSource.CLOSE,
            IndicatorParams.of(Map.of("k", 14, "kSmooth", 3, "dSmooth", 3))));

    // BB
    assertEquals(
        "BB (20)",
        IndicatorMapper.label(
            IndicatorType.BB, PriceSource.CLOSE, IndicatorParams.of(Map.of("period", 20))));

    // VWBB
    assertEquals(
        "VWBB (20)",
        IndicatorMapper.label(
            IndicatorType.VWBB, PriceSource.CLOSE, IndicatorParams.of(Map.of("period", 20))));
  }

  @Test
  void testConstructorIsPrivate() throws Exception {

    java.lang.reflect.Constructor<IndicatorMapper> constructor =
        IndicatorMapper.class.getDeclaredConstructor();

    assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));

    constructor.setAccessible(true);

    try {

      constructor.newInstance();

      fail("Should throw exception");

    } catch (java.lang.reflect.InvocationTargetException e) {

      assertInstanceOf(UnsupportedOperationException.class, e.getCause());
    }
  }
}
