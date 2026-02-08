package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.Renko;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RenkoRepository extends JpaRepository<Renko, Long> {

    List<Renko> findByTickerOrderByRenkoDateAsc(Ticker ticker);

    @Modifying
    @Transactional
    void deleteByTicker(Ticker ticker);

}
