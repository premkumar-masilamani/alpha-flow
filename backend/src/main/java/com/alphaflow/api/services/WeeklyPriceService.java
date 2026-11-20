package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WeeklyPriceService {

  private final WeeklyPriceRepository weeklyPriceRepository;

  public WeeklyPriceService(WeeklyPriceRepository weeklyPriceRepository) {
    this.weeklyPriceRepository = weeklyPriceRepository;
  }

  public List<OhlcvDto> getWeeklyPrice(Ticker ticker, int page, int size) {
    return weeklyPriceRepository.findLatestByTicker(ticker, PageRequest.of(page, size)).stream()
        .sorted(Comparator.comparing(WeeklyPrice::getPriceDate))
        .map(OhlcvMapper::toDto)
        .toList();
  }
}
