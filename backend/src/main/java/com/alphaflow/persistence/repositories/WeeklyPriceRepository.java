package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyPriceRepository extends JpaRepository<WeeklyPrice, Long> {

    Optional<WeeklyPrice> findTopByTickerOrderByPriceDateDesc(Ticker ticker);

    List<WeeklyPrice> findByTickerAndPriceDateGreaterThanEqual(Ticker ticker, LocalDate priceDate);

}
