package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.enums.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {

    Optional<Ticker> findByTickerSymbol(String symbol);

    List<Ticker> findByIsActiveTrue();

    List<Ticker> findByIsActiveTrueAndSource(DataSource source);

}
