package com.alphaflow.engine.calculators.indicators;


import org.junit.jupiter.api.Test;


import java.util.Map;


import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;


class IndicatorParamsTest {


  @Test

  void testOf() {

    IndicatorParams params = IndicatorParams.of(Map.of("period", 14, "k", 3));

    assertEquals(14, params.getInt("period"));

    assertEquals(3, params.getInt("k"));

    assertEquals("k=3,period=14", params.canonical());

    assertEquals("k=3,period=14", params.toString());

  }


  @Test

  void testParseEmpty() {

    IndicatorParams paramsNull = IndicatorParams.parse(null);

    assertEquals("", paramsNull.canonical());


    IndicatorParams paramsBlank = IndicatorParams.parse("   ");

    assertEquals("", paramsBlank.canonical());

  }


  @Test

  void testParseValid() {

    IndicatorParams params = IndicatorParams.parse("fast=12, slow=26, signal=9");

    assertEquals(12, params.getInt("fast"));

    assertEquals(26, params.getInt("slow"));

    assertEquals(9, params.getInt("signal"));

    assertEquals("fast=12,signal=9,slow=26", params.canonical());

  }


  @Test

  void testParseMalformed() {

    assertThrows(IllegalArgumentException.class, () -> IndicatorParams.parse("fast=12,slow"));

    assertThrows(IllegalArgumentException.class, () -> IndicatorParams.parse("fast=12,slow=abc"));

  }


  @Test

  void testGetIntRequiredAndDefault() {

    IndicatorParams params = IndicatorParams.of(Map.of("period", 14));

    assertEquals(14, params.getInt("period"));

    assertThrows(IllegalArgumentException.class, () -> params.getInt("nonexistent"));

    assertEquals(20, params.getInt("nonexistent", 20));

  }

}

