package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestCagr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestCagrRepository extends JpaRepository<BacktestCagr, Long> {

}
