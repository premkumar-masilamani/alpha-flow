package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.DataProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {

  Optional<Ticker> findByTickerSymbolIgnoreCase(String symbol);

  List<Ticker> findByIsActiveTrue();

  List<Ticker> findByIsActiveTrueAndCountryAndDataProvider(
      Country country, DataProvider dataProvider);
}
