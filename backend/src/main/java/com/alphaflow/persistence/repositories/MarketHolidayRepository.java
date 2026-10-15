package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.enums.Country;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, Long> {

  List<MarketHoliday> findAllByOrderByHolidayDateAsc();

  List<MarketHoliday> findByCountryOrderByHolidayDateAsc(Country country);

  List<MarketHoliday> findByCountryAndHolidayDateBetweenOrderByHolidayDateAsc(
      Country country, LocalDate startDate, LocalDate endDate);

  List<MarketHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(
      LocalDate startDate, LocalDate endDate);
}
