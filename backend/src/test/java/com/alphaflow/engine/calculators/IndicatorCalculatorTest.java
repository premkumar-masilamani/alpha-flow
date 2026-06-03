package com.alphaflow.engine.calculators;

import static org.mockito.Mockito.*;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndicatorCalculatorTest {

  @Test
  void testComputeIndicatorsSuccessAndErrorHandling() {

    TickerRepository tickerRepo = mock(TickerRepository.class);

    IndicatorTickerProcessor processor = mock(IndicatorTickerProcessor.class);

    Ticker t1 = new Ticker();

    t1.setTickerSymbol("AAPL");

    Ticker t2 = new Ticker();

    t2.setTickerSymbol("MSFT");

    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

    doThrow(new RuntimeException("Computation error")).when(processor).processTicker(t1);

    IndicatorCalculator calculator = new IndicatorCalculator(tickerRepo, processor);

    calculator.computeIndicators();

    verify(processor).processTicker(t1);

    verify(processor).processTicker(t2);
  }
}
