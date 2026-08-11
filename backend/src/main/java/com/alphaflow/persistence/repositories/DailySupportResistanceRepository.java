package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DailySupportResistanceRepository
    extends JpaRepository<DailySupportResistance, Long> {

  List<DailySupportResistance> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);
}
