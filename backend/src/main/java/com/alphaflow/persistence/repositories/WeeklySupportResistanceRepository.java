package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.WeeklySupportResistance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklySupportResistanceRepository extends JpaRepository<WeeklySupportResistance, Long> {
  List<WeeklySupportResistance> findByTicker_TickerSymbol(String tickerSymbol);
  
  void deleteByTicker_TickerSymbol(String tickerSymbol);
}
