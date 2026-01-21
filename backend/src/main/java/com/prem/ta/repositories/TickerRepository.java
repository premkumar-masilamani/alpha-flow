package com.prem.ta.repositories;

import com.prem.ta.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {

    Optional<Ticker> findByTickerSymbol(String symbol);

    List<Ticker> findByIsActiveTrue();

}
