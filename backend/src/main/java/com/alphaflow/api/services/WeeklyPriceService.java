package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
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

  public WeeklyPriceService(WeeklyPriceRepository weeklyPriceRepository) {
    this.weeklyPriceRepository = weeklyPriceRepository;
  }

  public List<OhlcvDto> getWeeklyPrice(Ticker ticker, int page, int size) {
    log.debug(
        "Fetching weekly prices for ticker {} (page={}, size={})",
        ticker.getTickerSymbol(),
        page,
        size);
    List<OhlcvDto> candles =
        weeklyPriceRepository.findLatestByTicker(ticker, PageRequest.of(page, size)).stream()
            .sorted(Comparator.comparing(WeeklyPrice::getPriceDate))
            .map(OhlcvMapper::toDto)
            .toList();
    log.debug(
        "Retrieved {} weekly price candles for ticker {} (page={}, size={})",
        candles.size(),
        ticker.getTickerSymbol(),
        page,
        size);
    return candles;
  }
}
