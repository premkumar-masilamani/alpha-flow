package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyPriceRepository extends JpaRepository<WeeklyPrice, Long> {

    Optional<WeeklyPrice> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

    Optional<WeeklyPrice> findTopByTickerOrderByPriceDateDesc(Ticker ticker);

}
