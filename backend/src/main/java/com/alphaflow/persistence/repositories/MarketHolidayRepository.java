package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.MarketHoliday;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, Long> {

  List<MarketHoliday> findAllByOrderByHolidayDateAsc();

  List<MarketHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(
      LocalDate startDate, LocalDate endDate);
}
