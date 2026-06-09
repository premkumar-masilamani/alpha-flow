package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorParamKey;
import org.junit.jupiter.api.Test;

class IndicatorEnumsTest {

  @Test
  void testIndicatorParamKeyValuesMatchDatabaseSchema() {
    assertEquals("period", IndicatorParamKey.PERIOD.getValue());
    assertEquals("fast", IndicatorParamKey.FAST.getValue());
    assertEquals("slow", IndicatorParamKey.SLOW.getValue());
    assertEquals("signal", IndicatorParamKey.SIGNAL.getValue());
    assertEquals("k", IndicatorParamKey.K.getValue());
    assertEquals("kSmooth", IndicatorParamKey.K_SMOOTH.getValue());
    assertEquals("dSmooth", IndicatorParamKey.D_SMOOTH.getValue());
  }

  @Test
  void testIndicatorOutputKeyValuesMatchDatabaseSchema() {
    assertEquals("value", IndicatorOutputKey.VALUE.getValue());
    assertEquals("macd", IndicatorOutputKey.MACD.getValue());
    assertEquals("signal", IndicatorOutputKey.SIGNAL.getValue());
    assertEquals("histogram", IndicatorOutputKey.HISTOGRAM.getValue());
    assertEquals("k", IndicatorOutputKey.K.getValue());
    assertEquals("d", IndicatorOutputKey.D.getValue());
  }
}
