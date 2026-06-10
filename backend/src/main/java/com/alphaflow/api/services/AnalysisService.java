package com.alphaflow.api.services;

import com.alphaflow.api.dtos.ASTAResponseDTO;
import com.alphaflow.persistence.entities.ASTAResults;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.ASTAResultsRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves the technical analysis results for a ticker, mapping ASTAResults to ASTAResponseDTOs. */
@Service
@Transactional(readOnly = true)
public class AnalysisService {

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final ASTAResultsRepository astaResultsRepository;

  public AnalysisService(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      ASTAResultsRepository astaResultsRepository) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.astaResultsRepository = astaResultsRepository;
  }

  public ASTAResponseDTO getAnalysis(String symbol) {
    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    DailyPrice latestPrice =
        dailyPriceRepository
            .findTopByTickerOrderByPriceDateDesc(ticker)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No daily price data found for symbol: " + symbol));
    LocalDate latestPriceDate = latestPrice.getPriceDate();

    ASTAResults res =
        astaResultsRepository
            .findTopByTickerOrderByPriceDateDesc(ticker)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Analysis result not found or stale for symbol: " + symbol));

    if (res.getPriceDate().isBefore(latestPriceDate)) {
      throw new ResourceNotFoundException("Analysis result is stale for symbol: " + symbol);
    }

    return toDTO(res, symbol);
  }

  private ASTAResponseDTO toDTO(ASTAResults res, String symbol) {
    return ASTAResponseDTO.builder()
        .symbol(symbol)
        .priceDate(res.getPriceDate())
        .emaSignal(res.getEmaSignal())
        .emaValue(res.getEmaValue())
        .macdSignal(res.getMacdSignal())
        .macdValue(res.getMacdValue())
        .stochasticSignal(res.getStochasticSignal())
        .stochasticValue(res.getStochasticValue())
        .rsiSignal(res.getRsiSignal())
        .rsiValue(res.getRsiValue())
        .volumeSignal(res.getVolumeSignal())
        .volumeValue(res.getVolumeValue())
        .overallSignal(res.getOverallSignal())
        .build();
  }
}
