package com.alphaflow.api.services;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.api.mappers.MarketHolidayMapper;
import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.enums.Country;
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

  public List<MarketHolidayDto> getHolidays(Country country, Integer year) {
    log.debug("Fetching market holidays for country: {}, year: {}", country, year);

    List<MarketHoliday> holidayEntities;
    if (country != null && year != null) {
      LocalDate startDate = LocalDate.of(year, 1, 1);
      LocalDate endDate = LocalDate.of(year, 12, 31);
      holidayEntities =
          marketHolidayRepository.findByCountryAndHolidayDateBetweenOrderByHolidayDateAsc(
              country, startDate, endDate);
    } else if (country != null) {
      holidayEntities = marketHolidayRepository.findByCountryOrderByHolidayDateAsc(country);
    } else if (year != null) {
      LocalDate startDate = LocalDate.of(year, 1, 1);
      LocalDate endDate = LocalDate.of(year, 12, 31);
      holidayEntities =
          marketHolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate);
    } else {
      holidayEntities = marketHolidayRepository.findAllByOrderByHolidayDateAsc();
    }

    return holidayEntities.stream().map(MarketHolidayMapper::toDto).toList();
  }
}
