package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves the daily price candles for a ticker, mapping the daily price entities to OhlcvDTOs. */
@Service
@Transactional(readOnly = true)
public class DailyPriceService {

  private final DailyPriceRepository dailyPriceRepository;

  public DailyPriceService(DailyPriceRepository dailyPriceRepository) {
    this.dailyPriceRepository = dailyPriceRepository;
  }

  public List<OhlcvDTO> getDailyPrice(Ticker ticker, int page, int size) {
    return getDailyPrice(ticker, LocalDate.now(), page, size);
  }

  public List<OhlcvDTO> getDailyPrice(Ticker ticker, LocalDate endDate, int page, int size) {
    return dailyPriceRepository
        .findLatestByTickerAndEndDate(ticker, endDate, PageRequest.of(page, size))
        .stream()
        .sorted(Comparator.comparing(DailyPrice::getPriceDate))
        .map(OhlcvMapper::toDTO)
        .toList();
  }
}
