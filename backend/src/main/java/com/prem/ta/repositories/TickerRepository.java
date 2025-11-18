package com.prem.ta.repositories;

import com.prem.ta.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Integer> {

}
