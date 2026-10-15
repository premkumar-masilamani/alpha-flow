package com.alphaflow.api.services;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.api.mappers.MarketHolidayMapper;
import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.repositories.MarketHolidayRepository;
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
    log.debug("Fetching all market holidays");
    List<MarketHoliday> holidayEntities = marketHolidayRepository.findAllByOrderByHolidayDateAsc();
    return holidayEntities.stream().map(MarketHolidayMapper::toDto).toList();
  }
}
