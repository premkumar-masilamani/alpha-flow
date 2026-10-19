package com.alphaflow.api.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.alphaflow.api.dtos.IndicatorConfigDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.persistence.entities.IndicatorDefinition;
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
  }

  @Test
  void testToConfigDtoWithBoundsAndWithoutBounds() {
    IndicatorDefinition boundedDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(1L)
            .indicatorType(IndicatorType.RSI)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 14))
            .bounds(Map.of("lower", 30, "upper", 70))
            .build();

    IndicatorConfigDto boundedDto = IndicatorMapper.toConfigDto(Timeframe.DAILY, boundedDef);
    assertEquals("DAILY", boundedDto.timeframe());
    assertEquals("RSI", boundedDto.type());
    assertEquals("CLOSE", boundedDto.source());
    assertEquals("period=14", boundedDto.params());
    assertEquals("lower=30,upper=70", boundedDto.bounds());

    IndicatorDefinition unboundedDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(2L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 20))
            .bounds(null)
            .build();

    IndicatorConfigDto unboundedDto = IndicatorMapper.toConfigDto(Timeframe.DAILY, unboundedDef);
    assertEquals("EMA", unboundedDto.type());
    assertEquals("period=20", unboundedDto.params());
    assertNull(unboundedDto.bounds());

    IndicatorDefinition emptyBoundsDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(3L)
            .indicatorType(IndicatorType.BB)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 20, "stdDev", 2))
            .bounds(Map.of())
            .build();

    IndicatorConfigDto emptyBoundsDto = IndicatorMapper.toConfigDto(Timeframe.DAILY, emptyBoundsDef);
    assertEquals("BB", emptyBoundsDto.type());
    assertNull(emptyBoundsDto.bounds());
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
