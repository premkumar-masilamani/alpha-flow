package com.alphaflow.persistence.repositories;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.SupportResistance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportResistanceRepository extends JpaRepository<SupportResistance, Long> {
  void deleteByTicker_TickerSymbol(String tickerSymbol);

  List<SupportResistance> findByTicker_TickerSymbolAndTimeframeAndFilterReasonIsNull(
      String tickerSymbol, Timeframe timeframe);

  List<SupportResistance> findByTicker_TickerSymbolAndFilterReasonIsNull(String tickerSymbol);
}
