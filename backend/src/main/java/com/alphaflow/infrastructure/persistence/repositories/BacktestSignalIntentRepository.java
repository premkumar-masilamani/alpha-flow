package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestSignalIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestSignalIntentRepository extends JpaRepository<BacktestSignalIntent, Long> {

}
