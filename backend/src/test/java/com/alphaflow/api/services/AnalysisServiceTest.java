package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.ASTAResponseDTO;
import com.alphaflow.persistence.entities.ASTAResults;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.TradeAction;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.ASTAResultsRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisServiceTest {

  private static final String SYMBOL = "AAPL";
  private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);
  private static final LocalDate YESTERDAY = LocalDate.of(2026, 5, 29);

  private DailyPrice dPrice(LocalDate date, double o, double h, double l, double c, double v) {
    return DailyPrice.builder()
        .priceDate(date)
        .priceOpen(BigDecimal.valueOf(o))
        .priceHigh(BigDecimal.valueOf(h))
        .priceLow(BigDecimal.valueOf(l))
        .priceClose(BigDecimal.valueOf(c))
        .volume(BigDecimal.valueOf(v))
        .build();
  }

  @Test
  void testGetAnalysisSuccess() {
    TickerRepository tickerRepository = mock(TickerRepository.class);
    DailyPriceRepository dailyPriceRepository = mock(DailyPriceRepository.class);
    ASTAResultsRepository astaResultsRepository = mock(ASTAResultsRepository.class);

    AnalysisService analysisService =
        new AnalysisService(tickerRepository, dailyPriceRepository, astaResultsRepository);

    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));
    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.of(dPrice(TODAY, 100, 105, 95, 102, 1000)));

    ASTAResults result =
        ASTAResults.builder()
            .ticker(ticker)
            .priceDate(TODAY)
            .overallSignal(TradeAction.BUY)
            .macdValue("Positive Crossover")
            .build();
    when(astaResultsRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.of(result));

    ASTAResponseDTO response = analysisService.getAnalysis(SYMBOL);

    assertNotNull(response);
    assertEquals(TradeAction.BUY, response.overallSignal());
    assertEquals("Positive Crossover", response.macdValue());
  }

  @Test
  void testGetAnalysisThrowsNotFoundWhenMissing() {
    TickerRepository tickerRepository = mock(TickerRepository.class);
    DailyPriceRepository dailyPriceRepository = mock(DailyPriceRepository.class);
    ASTAResultsRepository astaResultsRepository = mock(ASTAResultsRepository.class);

    AnalysisService analysisService =
        new AnalysisService(tickerRepository, dailyPriceRepository, astaResultsRepository);

    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));
    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.of(dPrice(TODAY, 100, 105, 95, 102, 1000)));
    when(astaResultsRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> analysisService.getAnalysis(SYMBOL));
  }

  @Test
  void testGetAnalysisThrowsNotFoundWhenStale() {
    TickerRepository tickerRepository = mock(TickerRepository.class);
    DailyPriceRepository dailyPriceRepository = mock(DailyPriceRepository.class);
    ASTAResultsRepository astaResultsRepository = mock(ASTAResultsRepository.class);

    AnalysisService analysisService =
        new AnalysisService(tickerRepository, dailyPriceRepository, astaResultsRepository);

    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));

    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.of(dPrice(TODAY, 100, 105, 95, 102, 1000)));

    ASTAResults result =
        ASTAResults.builder()
            .ticker(ticker)
            .priceDate(YESTERDAY)
            .overallSignal(TradeAction.BUY)
            .build();
    when(astaResultsRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.of(result));

    assertThrows(ResourceNotFoundException.class, () -> analysisService.getAnalysis(SYMBOL));
  }
}
