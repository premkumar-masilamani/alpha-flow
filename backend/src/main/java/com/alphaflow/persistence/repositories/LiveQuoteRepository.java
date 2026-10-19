package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveQuoteRepository extends JpaRepository<LiveQuote, Long> {

  @EntityGraph(attributePaths = {"ticker"})
  Optional<LiveQuote> findByTicker(Ticker ticker);
}
