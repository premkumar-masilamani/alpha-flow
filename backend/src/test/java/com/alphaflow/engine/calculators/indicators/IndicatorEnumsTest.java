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
    assertEquals("stdDev", IndicatorParamKey.STD_DEV.getValue());
  }

  @Test
  void testIndicatorOutputKeyValuesMatchDatabaseSchema() {
    assertEquals("value", IndicatorOutputKey.VALUE.getValue());
    assertEquals("macd", IndicatorOutputKey.MACD.getValue());
    assertEquals("signal", IndicatorOutputKey.SIGNAL.getValue());
    assertEquals("histogram", IndicatorOutputKey.HISTOGRAM.getValue());
    assertEquals("k", IndicatorOutputKey.K.getValue());
    assertEquals("d", IndicatorOutputKey.D.getValue());
    assertEquals("upper", IndicatorOutputKey.UPPER.getValue());
    assertEquals("middle", IndicatorOutputKey.MIDDLE.getValue());
    assertEquals("lower", IndicatorOutputKey.LOWER.getValue());
    assertEquals("bandwidth", IndicatorOutputKey.BANDWIDTH.getValue());
  }

  @Test
  void testIndicatorParamKeyFrom() {
    assertEquals(IndicatorParamKey.PERIOD, IndicatorParamKey.from("period"));
    assertEquals(IndicatorParamKey.PERIOD, IndicatorParamKey.from("PERIOD"));
    org.junit.jupiter.api.Assertions.assertNull(IndicatorParamKey.from(null));
    org.junit.jupiter.api.Assertions.assertNull(IndicatorParamKey.from(""));
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> IndicatorParamKey.from("unknown"));
  }

  @Test
  void testIndicatorOutputKeyFrom() {
    assertEquals(IndicatorOutputKey.VALUE, IndicatorOutputKey.from("value"));
    assertEquals(IndicatorOutputKey.VALUE, IndicatorOutputKey.from("VALUE"));
    org.junit.jupiter.api.Assertions.assertNull(IndicatorOutputKey.from(null));
    org.junit.jupiter.api.Assertions.assertNull(IndicatorOutputKey.from(""));
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> IndicatorOutputKey.from("unknown"));
  }
}
