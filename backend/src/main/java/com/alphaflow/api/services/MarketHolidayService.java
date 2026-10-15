package com.alphaflow.api.services;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.api.mappers.MarketHolidayMapper;
import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.repositories.MarketHolidayRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class MarketHolidayService {

  private final MarketHolidayRepository marketHolidayRepository;

  public MarketHolidayService(MarketHolidayRepository marketHolidayRepository) {
    this.marketHolidayRepository = marketHolidayRepository;
  }

  public List<MarketHolidayDto> getHolidays() {
    int currentYear = LocalDate.now().getYear();
    log.debug("Fetching market holidays for all countries for current year: {}", currentYear);

    LocalDate startDate = LocalDate.of(currentYear, 1, 1);
    LocalDate endDate = LocalDate.of(currentYear, 12, 31);
    List<MarketHoliday> holidayEntities =
        marketHolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate);

    return holidayEntities.stream().map(MarketHolidayMapper::toDto).toList();
  }
}
