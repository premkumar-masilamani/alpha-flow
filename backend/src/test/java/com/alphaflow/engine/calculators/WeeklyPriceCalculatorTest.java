package com.alphaflow.engine.calculators;


import com.alphaflow.persistence.entities.Ticker;

import com.alphaflow.persistence.repositories.TickerRepository;

import org.junit.jupiter.api.Test;


import java.util.List;


import static org.mockito.Mockito.*;


class WeeklyPriceCalculatorTest {


  @Test

  void testComputeWeeklyPricesSuccessAndErrorHandling() {

    TickerRepository tickerRepo = mock(TickerRepository.class);

    WeeklyTickerProcessor processor = mock(WeeklyTickerProcessor.class);


    Ticker t1 = new Ticker();

    t1.setTickerSymbol("AAPL");

    Ticker t2 = new Ticker();

    t2.setTickerSymbol("MSFT");


    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

    doThrow(new RuntimeException("Computation error")).when(processor).processTicker(t1);


    WeeklyPriceCalculator calculator = new WeeklyPriceCalculator(tickerRepo, processor);

    calculator.computeWeeklyPrices();


    verify(processor).processTicker(t1);

    verify(processor).processTicker(t2);

  }

}

