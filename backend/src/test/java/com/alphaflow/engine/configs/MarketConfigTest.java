package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarketConfigTest {

  @Test
  void testGettersAndSetters() {
    MarketConfig config = new MarketConfig();
    List<String> usHolidays = List.of("01-01", "12-25");
    List<String> inHolidays = List.of("01-26", "08-15");

    config.setUs(usHolidays);
    config.setIn(inHolidays);

    assertEquals(usHolidays, config.getUs());
    assertEquals(inHolidays, config.getIn());
  }
}
