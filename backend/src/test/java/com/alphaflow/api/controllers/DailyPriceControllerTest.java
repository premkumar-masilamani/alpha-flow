package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.DailyPriceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyPriceControllerTest {

  @Test
  void testGetDailyPriceDataForTicker() {

    DailyPriceService service = mock(DailyPriceService.class);

    OhlcvDTO dto =
        new OhlcvDTO(
            LocalDate.of(2026, 5, 29),
            new BigDecimal("100.00"),
            new BigDecimal("105.00"),
            new BigDecimal("99.00"),
            new BigDecimal("102.00"),
            new BigDecimal("1000.00"));

    when(service.getDailyPriceByTickerName("AAPL", 0, 250)).thenReturn(List.of(dto));

    DailyPriceController controller = new DailyPriceController(service);

    List<OhlcvDTO> res = controller.getDailyPriceDataForTicker("AAPL", 0, 250);

    assertEquals(1, res.size());

    assertEquals(LocalDate.of(2026, 5, 29), res.get(0).priceDate());
  }
}
