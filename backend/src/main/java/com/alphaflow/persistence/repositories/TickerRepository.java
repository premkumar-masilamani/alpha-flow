package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {

  Optional<Ticker> findByTickerSymbol(String symbol);

  Optional<Ticker> findByTickerSymbolIgnoreCase(String symbol);

  boolean existsByTickerSymbolIgnoreCase(String symbol);
  List<Ticker> findByIsActiveTrue();

  List<Ticker> findByIsActiveTrue(org.springframework.data.domain.Pageable pageable);
}
