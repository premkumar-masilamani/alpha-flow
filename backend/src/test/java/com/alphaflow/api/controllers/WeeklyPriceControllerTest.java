package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.WeeklyPriceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyPriceControllerTest {

  @Test
  void testGetWeeklyDataForTicker() {

    WeeklyPriceService service = mock(WeeklyPriceService.class);

    OhlcvDTO dto =
        new OhlcvDTO(
            LocalDate.of(2026, 5, 29),
            new BigDecimal("100.00"),
            new BigDecimal("105.00"),
            new BigDecimal("99.00"),
            new BigDecimal("102.00"),
            new BigDecimal("1000.00"));

    when(service.getWeeklyPriceByTickerName("AAPL", 0, 250)).thenReturn(List.of(dto));

    WeeklyPriceController controller = new WeeklyPriceController(service);

    List<OhlcvDTO> res = controller.getWeeklyDataForTicker("AAPL", 0, 250);

    assertEquals(1, res.size());

    assertEquals(LocalDate.of(2026, 5, 29), res.getFirst().priceDate());
  }
}
