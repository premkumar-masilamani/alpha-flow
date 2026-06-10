package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.ASTAResponseDTO;
import com.alphaflow.engine.strategies.ASTAStrategy;
import com.alphaflow.persistence.enums.TradeAction;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AnalysisControllerTest {

  @Test
  void testGetTechnicalAnalysisForTicker() {

    ASTAStrategy strategy = mock(ASTAStrategy.class);

    ASTAResponseDTO mockResponse =
        ASTAResponseDTO.builder()
            .symbol("AAPL")
            .priceDate(LocalDate.of(2026, 5, 30))
            .emaSignal(TradeAction.BUY)
            .emaValue("Bullish")
            .macdSignal(TradeAction.BUY)
            .macdValue("Positive Crossover")
            .stochasticSignal(TradeAction.BUY)
            .stochasticValue("Positive Crossover")
            .rsiSignal(TradeAction.BUY)
            .rsiValue("Uptick")
            .volumeSignal(TradeAction.BUY)
            .volumeValue("Heavy")
            .overallSignal(TradeAction.BUY)
            .build();

    when(strategy.getAnalysis("AAPL")).thenReturn(mockResponse);

    AnalysisController controller = new AnalysisController(strategy);

    ASTAResponseDTO result = controller.getTechnicalAnalysisForTicker("AAPL");

    assertNotNull(result);

    assertEquals("AAPL", result.symbol());

    assertEquals(TradeAction.BUY, result.overallSignal());
  }
}
