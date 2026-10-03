package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IntradayPriceRepository extends JpaRepository<IntradayPrice, Long> {

  @Query(
      """
        SELECT ip FROM IntradayPrice ip
        JOIN FETCH ip.ticker tk
        WHERE ip.ticker = :ticker AND ip.timeframe = :timeframe
        ORDER BY ip.priceTime DESC
      """)
  List<IntradayPrice> findLatestByTickerAndTimeframe(
      Ticker ticker, String timeframe, Pageable pageable);

  Optional<IntradayPrice> findTopByTickerAndTimeframeOrderByPriceTimeDesc(
      Ticker ticker, String timeframe);

  Optional<IntradayPrice> findTopByTickerOrderByPriceTimeDesc(Ticker ticker);

  List<IntradayPrice> findByTickerAndTimeframeOrderByPriceTimeAsc(Ticker ticker, String timeframe);

  List<IntradayPrice> findByTickerAndTimeframeAndPriceTimeGreaterThanEqualOrderByPriceTimeAsc(
      Ticker ticker, String timeframe, OffsetDateTime startTime);

  boolean existsByTickerAndTimeframeAndPriceTime(
      Ticker ticker, String timeframe, OffsetDateTime priceTime);
}
