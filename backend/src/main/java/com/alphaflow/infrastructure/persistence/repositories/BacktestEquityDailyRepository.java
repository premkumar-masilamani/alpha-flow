package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestEquityDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestEquityDailyRepository extends JpaRepository<BacktestEquityDaily, Long> {

}
