package com.alphaflow.api.services;

import com.alphaflow.api.configs.ApiProperties;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DailyPriceService {

  private static final Logger log = LoggerFactory.getLogger(DailyPriceService.class);

  private final DailyPriceRepository dailyPriceRepository;
  private final TickerRepository tickerRepository;
  private final ApiProperties apiProperties;

  public DailyPriceService(
      DailyPriceRepository dailyPriceRepository,
      TickerRepository tickerRepository,
      ApiProperties apiProperties) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.tickerRepository = tickerRepository;
    this.apiProperties = apiProperties;
  }

  public List<OhlcvDTO> getDailyPriceByTickerName(String tickerName) {
    return getDailyPriceByTickerName(tickerName, 0, null);
  }

  public List<OhlcvDTO> getDailyPriceByTickerName(String tickerName, int page, Integer size) {
    int window = apiProperties.windowFor(Timeframe.DAILY);
    if (window <= 0) {
      window = 180;
    }
    int actualSize = size != null ? Math.min(size, window * 5) : window;
    if (actualSize < 1) {
      actualSize = 1;
    }
    log.debug(
        "Fetching daily candle data for ticker: {} (page={}, size={})",
        tickerName,
        page,
        actualSize);
    // Match the case-insensitive lookup used by findLatestByTickerName below.
    if (!tickerRepository.existsByTickerSymbolIgnoreCase(tickerName)) {
      log.warn("Ticker not found for symbol: {}", tickerName);
      throw new ResourceNotFoundException("Ticker not found: " + tickerName);
    }
    return dailyPriceRepository
        .findLatestByTickerName(tickerName, PageRequest.of(page, actualSize))
        .stream()
        .sorted(Comparator.comparing(DailyPrice::getPriceDate))
        .map(OhlcvMapper::toDTO)
        .toList();
  }
}
