package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTrade, UUID> {

}
