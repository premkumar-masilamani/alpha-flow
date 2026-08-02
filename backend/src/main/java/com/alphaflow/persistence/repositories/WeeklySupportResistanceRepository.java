package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklySupportResistanceRepository
    extends JpaRepository<WeeklySupportResistance, Long> {

  List<WeeklySupportResistance> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  @Modifying
  @Query("DELETE FROM WeeklySupportResistance wsr WHERE wsr.ticker = :ticker")
  void deleteByTicker(Ticker ticker);
}
