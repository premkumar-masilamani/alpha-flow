package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class WeeklyPriceService {

  private final WeeklyPriceRepository weeklyPriceRepository;
  private final TickerRepository tickerRepository;

  public WeeklyPriceService(
      WeeklyPriceRepository weeklyPriceRepository, TickerRepository tickerRepository) {
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.tickerRepository = tickerRepository;
  }

  public List<OhlcvDto> getWeeklyPrice(Ticker ticker, int page, int size) {
    return weeklyPriceRepository.findLatestByTicker(ticker, PageRequest.of(page, size)).stream()
        .sorted(Comparator.comparing(WeeklyPrice::getPriceDate))
        .map(OhlcvMapper::toDto)
        .toList();
  }

  public List<OhlcvDto> getWeeklyPriceByTickerName(String tickerName, int page, int size) {
    log.debug(
        "Fetching weekly candle data for ticker: {} (page={}, size={})", tickerName, page, size);
    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(tickerName)
            .orElseThrow(
                () -> {
                  log.warn("Ticker not found for symbol: {}", tickerName);
                  return new ResourceNotFoundException("Ticker not found: " + tickerName);
                });
    return getWeeklyPrice(ticker, page, size);
  }
}
