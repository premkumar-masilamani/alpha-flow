package com.prem.ta.repositories;

import com.prem.ta.entities.TradeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeDataRepository extends JpaRepository<TradeData, Long> {

    @Query("""
                SELECT t FROM TradeData t
                JOIN FETCH t.ticker tk
                WHERE LOWER(tk.symbol) = LOWER(:tickerName)
                ORDER BY t.tradeDate ASC
            """)
    List<TradeData> findAllByTickerNameWithTicker(String tickerName);

}
