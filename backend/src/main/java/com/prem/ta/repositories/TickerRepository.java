package com.prem.ta.repositories;

import com.prem.ta.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {

    // Lookup by symbol
    Optional<Ticker> findBySymbol(String symbol);

    // Prevent duplicates (case-insensitive symbols)
    boolean existsBySymbolIgnoreCase(String symbol);
}
