package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.MarketHoliday;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, Long> {

  List<MarketHoliday> findAllByOrderByHolidayDateAsc();
}
