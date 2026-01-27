package com.alphaflow.core;

import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.RenkoDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import com.alphaflow.infrastructure.util.RenkoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static java.time.LocalDate.EPOCH;

@Service
public class RenkoDataComputer {

    private static final Logger log = LoggerFactory.getLogger(RenkoDataComputer.class);

    private final MarketDataRepository marketDataRepository;
    private final RenkoDataRepository renkoDataRepository;
    private final TickerRepository tickerRepository;

    public RenkoDataComputer(
            MarketDataRepository marketDataRepository,
            RenkoDataRepository renkoDataRepository,
            TickerRepository tickerRepository
    ) {
        this.marketDataRepository = marketDataRepository;
        this.renkoDataRepository = renkoDataRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public void compute() {
        log.info("Starting Renko Data Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Renko Data for {}", ticker.getTickerSymbol());

            List<MarketData> allSeries = marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                    ticker, EPOCH);

            if (allSeries.isEmpty()) {
                log.error("No market data found for {}", ticker.getTickerSymbol());
                return;
            }

            // TODO: Add logic to detect existing Renko data and NOT reprocess it

            renkoDataRepository.deleteByTicker(ticker);
            renkoDataRepository.flush();

            BigDecimal brickSize = RenkoUtil.calculateBrickSize(allSeries);
            List<RenkoData> renkoBricks = RenkoUtil.generateRenkoBricks(ticker, allSeries, brickSize);

            if (renkoBricks.isEmpty()) {
                log.error("No Renko bricks generated for {}", ticker.getTickerSymbol());
                return;
            }

            renkoDataRepository.saveAll(renkoBricks);
            log.info("Generated {} Renko bricks for {}", renkoBricks.size(), ticker.getTickerSymbol());
        });

        log.info("Completed Renko Data Computation");
    }
}
