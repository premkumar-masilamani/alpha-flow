package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IntradayPriceService {

  private final IntradayPriceRepository intradayPriceRepository;

  public IntradayPriceService(IntradayPriceRepository intradayPriceRepository) {
    this.intradayPriceRepository = intradayPriceRepository;
  }

  public List<OhlcvDto> getIntradayPrice(Ticker ticker, String timeframe, int page, int size) {
    if (timeframe == null || timeframe.trim().isEmpty()) {
      throw new IllegalArgumentException("Timeframe must not be null or blank");
    }
    return intradayPriceRepository
        .findLatestByTickerAndTimeframe(ticker, timeframe, PageRequest.of(page, size))
        .stream()
        .sorted(Comparator.comparing(IntradayPrice::getPriceTime))
        .map(OhlcvMapper::toDto)
        .toList();
  }
}
