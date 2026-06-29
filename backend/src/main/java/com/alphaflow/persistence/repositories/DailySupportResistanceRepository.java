package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailySupportResistance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailySupportResistanceRepository extends JpaRepository<DailySupportResistance, Long> {
  List<DailySupportResistance> findByTicker_TickerSymbol(String tickerSymbol);
  
  void deleteByTicker_TickerSymbol(String tickerSymbol);
}
