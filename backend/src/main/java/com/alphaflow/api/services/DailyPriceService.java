package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.time.LocalDate;
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

  public DailyPriceService(DailyPriceRepository dailyPriceRepository) {
    this.dailyPriceRepository = dailyPriceRepository;
  }

  public List<OhlcvDto> getDailyPrice(Ticker ticker, int page, int size) {
    return getDailyPrice(ticker, LocalDate.now(), page, size);
  }

  public List<OhlcvDto> getDailyPrice(Ticker ticker, LocalDate endDate, int page, int size) {
    log.debug(
        "Fetching daily prices for ticker {} up to {} (page={}, size={})",
        ticker.getTickerSymbol(),
        endDate,
        page,
        size);
    List<OhlcvDto> candles =
        dailyPriceRepository
            .findLatestByTickerAndEndDate(ticker, endDate, PageRequest.of(page, size))
            .stream()
            .sorted(Comparator.comparing(DailyPrice::getPriceDate))
            .map(OhlcvMapper::toDto)
            .toList();
    log.debug(
        "Retrieved {} daily price candles for ticker {} (page={}, size={})",
        candles.size(),
        ticker.getTickerSymbol(),
        page,
        size);
    return candles;
  }
}
