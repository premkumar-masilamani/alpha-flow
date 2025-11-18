package com.prem.ta.repositories;

import com.prem.ta.entities.TradeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeDataRepository extends JpaRepository<TradeData, Integer> {

}
