package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailySupportResistanceRepository
    extends JpaRepository<DailySupportResistance, Long> {

  List<DailySupportResistance> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  List<DailySupportResistance> findByTickerAndPriceDateAndZoneMidpointBetween(
      Ticker ticker, LocalDate priceDate, BigDecimal minMidpoint, BigDecimal maxMidpoint);

  @Modifying
  @Query(
      """
      DELETE FROM DailySupportResistance dsr
      WHERE dsr.ticker = :ticker AND dsr.priceDate < :cutoffDate
      """)
  void deleteByTickerAndPriceDateLessThan(
      @Param("ticker") Ticker ticker, @Param("cutoffDate") LocalDate cutoffDate);
}
