package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RenkoDataRepository extends JpaRepository<RenkoData, Long> {

    List<RenkoData> findByTickerAndPriceSourceOrderByRenkoDateAsc(Ticker ticker, String priceSource);

    @Modifying
    @Transactional
    void deleteByTickerAndPriceSource(Ticker ticker, String priceSource);

}
