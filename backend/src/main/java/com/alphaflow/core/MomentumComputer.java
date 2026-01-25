package com.alphaflow.core;

import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MomentumComputer {

    private static final Logger log = LoggerFactory.getLogger(MomentumComputer.class);

    private final MarketStateRepository marketStateRepository;

    public MomentumComputer(MarketStateRepository marketStateRepository) {
        this.marketStateRepository = marketStateRepository;
    }

    /**
     * Computes Capital Momentum for all tickers and all dates that haven't been processed yet.
     * Capital Momentum is defined as EMA_10(T_CAP) - EMA_20(T_CAP).
     * This implementation uses a bulk SQL operation for maximum efficiency and scalability.
     */
    public void compute() {
        log.info("Starting bulk Capital Momentum computation...");
        try {
            int updatedRows = marketStateRepository.computeCapitalMomentumBulk();
            log.info("Capital Momentum computation completed. Added {} new records.", updatedRows);
        } catch (Exception e) {
            log.error("Failed to compute Capital Momentum in bulk", e);
            throw e;
        }
    }
}
