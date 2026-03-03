package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RenkoDataRepository extends JpaRepository<RenkoData, Long> {

    List<RenkoData> findByTickerOrderByRenkoDataDateAsc(Ticker ticker);
    Optional<RenkoData> findTopByTickerOrderByRenkoDataDateDesc(Ticker ticker);

}
