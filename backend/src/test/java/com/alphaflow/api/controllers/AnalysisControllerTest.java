package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.AnalysisResponseDTO;
import com.alphaflow.api.services.AnalysisService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AnalysisControllerTest {

  @Test
  void testGetTechnicalAnalysis() {
    AnalysisService service = mock(AnalysisService.class);
    AnalysisResponseDTO mockResponse =
        AnalysisResponseDTO.builder()
            .symbol("AAPL")
            .priceDate(LocalDate.of(2026, 5, 30))
            .emaSignal("BUY")
            .emaValue("Bullish")
            .macdSignal("BUY")
            .macdValue("Positive Crossover")
            .stochasticSignal("BUY")
            .stochasticValue("Positive Crossover")
            .rsiSignal("BUY")
            .rsiValue("Uptick")
            .volumeSignal("BUY")
            .volumeValue("Heavy")
            .overallSignal("BUY")
            .build();
    when(service.getAnalysis("AAPL")).thenReturn(mockResponse);

    AnalysisController controller = new AnalysisController(service);
    AnalysisResponseDTO result = controller.getTechnicalAnalysis("AAPL");

    assertNotNull(result);
    assertEquals("AAPL", result.symbol());
    assertEquals("BUY", result.overallSignal());
  }
}
