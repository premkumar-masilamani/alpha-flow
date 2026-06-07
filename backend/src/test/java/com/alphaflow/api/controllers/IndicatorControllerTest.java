package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.persistence.enums.Timeframe;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndicatorControllerTest {

  @Test
  void testGetConfiguredIndicators() {

    IndicatorService service = mock(IndicatorService.class);

    IndicatorConfigDTO configDto =
        new IndicatorConfigDTO("DAILY", "EMA", "CLOSE", "period=14", "EMA (14)");

    when(service.getConfiguredIndicators()).thenReturn(List.of(configDto));

    IndicatorController controller = new IndicatorController(service);

    List<IndicatorConfigDTO> res = controller.getConfiguredIndicators();

    assertEquals(1, res.size());

    assertEquals("EMA", res.get(0).type());
  }

  @Test
  void testGetIndicatorSeriesValid() {

    IndicatorService service = mock(IndicatorService.class);

    IndicatorSeriesDTO seriesDto =
        new IndicatorSeriesDTO("EMA", "CLOSE", "period=14", "EMA (14)", List.of());

    when(service.getIndicatorSeries("AAPL", Timeframe.DAILY, 0, 250))
        .thenReturn(List.of(seriesDto));

    IndicatorController controller = new IndicatorController(service);

    List<IndicatorSeriesDTO> res = controller.getIndicatorSeries("AAPL", "DAILY", 0, 250);

    assertEquals(1, res.size());

    assertEquals("EMA", res.get(0).type());

    List<IndicatorSeriesDTO> resLowercase =
        controller.getIndicatorSeries("AAPL", "  daily ", 0, 250);

    assertEquals(1, resLowercase.size());
  }

  @Test
  void testGetIndicatorSeriesInvalidTimeframe() {

    IndicatorService service = mock(IndicatorService.class);

    IndicatorController controller = new IndicatorController(service);

    assertThrows(
        IllegalArgumentException.class,
        () -> controller.getIndicatorSeries("AAPL", "HOURLY", 0, 250));

    assertThrows(
        IllegalArgumentException.class, () -> controller.getIndicatorSeries("AAPL", null, 0, 250));

    assertThrows(
        IllegalArgumentException.class,
        () -> controller.getIndicatorSeries("AAPL", "   ", 0, 250));
  }
}
