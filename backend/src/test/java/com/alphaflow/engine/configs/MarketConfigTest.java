package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class MarketConfigTest {

  @Test
  void testConstants() {
    assertEquals("Asia/Kolkata", MarketConfig.TIMEZONE_KOLKATA);
    assertEquals(ZoneId.of("Asia/Kolkata"), MarketConfig.IST_ZONE);
    assertEquals("America/New_York", MarketConfig.TIMEZONE_NEW_YORK);
    assertEquals(ZoneId.of("America/New_York"), MarketConfig.EST_ZONE);
    assertEquals(LocalTime.of(15, 30), MarketConfig.IN_MARKET_CLOSE_TIME);
    assertEquals(LocalTime.of(16, 0), MarketConfig.US_MARKET_CLOSE_TIME);
  }

  @Test
  void testPrivateConstructor() throws Exception {
    Constructor<MarketConfig> constructor = MarketConfig.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    MarketConfig instance = constructor.newInstance();
    assertNotNull(instance);
  }
}
