package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RenkoBrickRepository extends JpaRepository<RenkoData, Long> {

    List<RenkoData> findByTickerOrderByRenkoBrickDateAsc(Ticker ticker);

    @Modifying
    @Transactional
    void deleteByTicker(Ticker ticker);

}
