package com.prem.ta.repositories;

import com.prem.ta.entities.TradeData;
import com.prem.ta.entities.TradeDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeDataRepository extends JpaRepository<TradeData, TradeDataId> {
}
