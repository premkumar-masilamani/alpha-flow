package com.alphaflow.api.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alphaflow.persistence.enums.IndicatorOutputKey;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IndicatorPointDTOTest {

  @Test
  void testGetValue() {
    IndicatorPointDTO point =
        IndicatorPointDTO.builder()
            .date(LocalDate.now())
            .values(
                Map.of(
                    "macd", new BigDecimal("1.5"),
                    "signal", new BigDecimal("1.2"),
                    "histogram", new BigDecimal("0.3")))
            .build();

    assertEquals(new BigDecimal("1.5"), point.getValue(IndicatorOutputKey.MACD));
    assertEquals(new BigDecimal("1.2"), point.getValue(IndicatorOutputKey.SIGNAL));
    assertEquals(new BigDecimal("0.3"), point.getValue(IndicatorOutputKey.HISTOGRAM));
    assertNull(point.getValue(IndicatorOutputKey.VALUE));
  }

  @Test
  void testGetValueWithNullValues() {
    IndicatorPointDTO point =
        IndicatorPointDTO.builder().date(LocalDate.now()).values(null).build();
    assertNull(point.getValue(IndicatorOutputKey.MACD));
  }
}
