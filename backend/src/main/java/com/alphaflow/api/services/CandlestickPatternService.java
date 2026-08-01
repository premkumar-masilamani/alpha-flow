package com.alphaflow.api.services;

import com.alphaflow.api.dtos.CandlestickPatternDTO;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.CandlestickPattern;
import com.alphaflow.persistence.enums.PatternSentiment;
import com.alphaflow.persistence.repositories.DailyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to query computed candlestick pattern records. Aligns pattern events to matching date
 * ranges of price page windows.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class CandlestickPatternService {

  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  private final WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;

  public CandlestickPatternService(
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyCandlestickPatternRepository dailyCandlestickPatternRepository,
      WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyCandlestickPatternRepository = dailyCandlestickPatternRepository;
    this.weeklyCandlestickPatternRepository = weeklyCandlestickPatternRepository;
  }

  /** Fetches candlestick patterns for a given ticker, timeframe, and page date range. */
  public List<CandlestickPatternDTO> getPatterns(
      Ticker ticker, Timeframe timeframe, int page, int size) {
    LocalDate endDate = LocalDate.now();
    PageRequest pageRequest = PageRequest.of(page, size);

    List<LocalDate> pageDates =
        timeframe == Timeframe.WEEKLY
            ? weeklyPriceRepository.findRecentPriceDatesUpTo(ticker, endDate, pageRequest)
            : dailyPriceRepository.findRecentPriceDatesUpTo(ticker, endDate, pageRequest);

    if (pageDates.isEmpty()) {
      return List.of();
    }

    LocalDate start = pageDates.getLast();
    LocalDate end = pageDates.getFirst();

    if (timeframe == Timeframe.WEEKLY) {
      return weeklyCandlestickPatternRepository.findSeriesBetween(ticker, start, end).stream()
          .map(r -> toDTO(r.getPriceDate(), r.getPattern(), r.getSentiment()))
          .toList();
    } else if (timeframe == Timeframe.DAILY) {
      return dailyCandlestickPatternRepository.findSeriesBetween(ticker, start, end).stream()
          .map(r -> toDTO(r.getPriceDate(), r.getPattern(), r.getSentiment()))
          .toList();
    }
    return List.of();
  }

  private CandlestickPatternDTO toDTO(
      LocalDate date, CandlestickPattern pattern, PatternSentiment sentiment) {
    return CandlestickPatternDTO.builder()
        .date(date)
        .shortName(pattern.getShortName())
        .longName(pattern.getLongName())
        .sentiment(sentiment)
        .build();
  }
}
