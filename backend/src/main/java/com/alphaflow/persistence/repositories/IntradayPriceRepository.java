package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IntradayPriceRepository extends JpaRepository<IntradayPrice, Long> {

  @Query(
      """
        SELECT tk, MAX(ip.priceTime)
        FROM Ticker tk
        LEFT JOIN IntradayPrice ip ON ip.ticker = tk AND ip.timeframe = :timeframe
        WHERE tk.isActive = true
          AND tk.dataProvider = :dataProvider
        GROUP BY tk
      """)
  List<Object[]> findLatestPriceTimesForActiveTickersQuery(
      @Param("dataProvider") DataProvider dataProvider, @Param("timeframe") String timeframe);

  default Map<Ticker, OffsetDateTime> findLatestPriceTimesForActiveTickers(
      DataProvider dataProvider, String timeframe) {
    Map<Ticker, OffsetDateTime> result = new LinkedHashMap<>();
    for (Object[] row : findLatestPriceTimesForActiveTickersQuery(dataProvider, timeframe)) {
      result.put((Ticker) row[0], (OffsetDateTime) row[1]);
    }
    return result;
  }

  default Map<Ticker, OffsetDateTime> findLatestPriceTimesForActiveTickers(String timeframe) {
    return findLatestPriceTimesForActiveTickers(DataProvider.ANGEL_ONE, timeframe);
  }

  @Query(
      """
        SELECT ip FROM IntradayPrice ip
        JOIN FETCH ip.ticker tk
        WHERE ip.ticker = :ticker AND ip.timeframe = :timeframe
        ORDER BY ip.priceTime DESC
      """)
  List<IntradayPrice> findLatestByTickerAndTimeframe(
      Ticker ticker, String timeframe, Pageable pageable);

  @Query(
      """
        SELECT ip FROM IntradayPrice ip
        JOIN FETCH ip.ticker tk
        WHERE ip.ticker = :ticker
          AND ip.timeframe = :timeframe
          AND ip.priceTime <= :endDateTime
        ORDER BY ip.priceTime DESC
      """)
  List<IntradayPrice> findLatestByTickerAndTimeframeAndEndDate(
      @Param("ticker") Ticker ticker,
      @Param("timeframe") String timeframe,
      @Param("endDateTime") OffsetDateTime endDateTime,
      Pageable pageable);

  Optional<IntradayPrice> findTopByTickerAndTimeframeOrderByPriceTimeDesc(
      Ticker ticker, String timeframe);

  Optional<IntradayPrice> findTopByTickerOrderByPriceTimeDesc(Ticker ticker);

  List<IntradayPrice> findByTickerAndTimeframeOrderByPriceTimeAsc(Ticker ticker, String timeframe);

  List<IntradayPrice> findByTickerAndTimeframeAndPriceTimeGreaterThanEqualOrderByPriceTimeAsc(
      Ticker ticker, String timeframe, OffsetDateTime startTime);

  boolean existsByTickerAndTimeframeAndPriceTime(
      Ticker ticker, String timeframe, OffsetDateTime priceTime);
}
