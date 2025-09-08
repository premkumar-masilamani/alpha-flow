package com.prem.ta.repositories;

import com.prem.ta.entities.Interval;
import com.prem.ta.entities.Ticker;
import com.prem.ta.entities.TradeData;
import com.prem.ta.entities.TradeDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TradeDataRepository extends JpaRepository<TradeData, TradeDataId> {

    // Get all trades for a ticker in a given interval
    List<TradeData> findByTickerAndIntervalOrderByTradeTimeAsc(Ticker ticker, Interval interval);

    // Time-bounded query
    List<TradeData> findByTickerAndIntervalAndTradeTimeBetweenOrderByTradeTimeAsc(
            Ticker ticker,
            Interval interval,
            OffsetDateTime start,
            OffsetDateTime end
    );

    // Latest trade
    TradeData findTopByTickerAndIntervalOrderByTradeTimeDesc(Ticker ticker, Interval interval);
}
