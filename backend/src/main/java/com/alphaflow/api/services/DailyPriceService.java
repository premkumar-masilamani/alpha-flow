package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class DailyPriceService {

  private final DailyPriceRepository dailyPriceRepository;
  private final TickerRepository tickerRepository;

  public DailyPriceService(
      DailyPriceRepository dailyPriceRepository, TickerRepository tickerRepository) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.tickerRepository = tickerRepository;
  }

  public List<OhlcvDTO> getDailyPriceByTickerName(String tickerName, int page, int size) {
    log.debug(
        "Fetching daily candle data for ticker: {} (page={}, size={})", tickerName, page, size);
    // Match the case-insensitive lookup used by findLatestByTickerName below.
    if (!tickerRepository.existsByTickerSymbolIgnoreCase(tickerName)) {
      log.warn("Ticker not found for symbol: {}", tickerName);
      throw new ResourceNotFoundException("Ticker not found: " + tickerName);
    }
    return dailyPriceRepository
        .findLatestByTickerName(tickerName, PageRequest.of(page, size))
        .stream()
        .sorted(Comparator.comparing(DailyPrice::getPriceDate))
        .map(OhlcvMapper::toDTO)
        .toList();
  }
}
