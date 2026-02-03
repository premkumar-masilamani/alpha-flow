package com.alphaflow.engine.calculation;

import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.generators.RenkoBricksGenerator;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.repositories.RenkoDataRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RenkoDataCalculator {

    private static final Logger log = LoggerFactory.getLogger(RenkoDataCalculator.class);

    private final MarketDataRepository marketDataRepository;
    private final RenkoDataRepository renkoDataRepository;
    private final TickerRepository tickerRepository;

    public RenkoDataCalculator(
            MarketDataRepository marketDataRepository,
            RenkoDataRepository renkoDataRepository,
            TickerRepository tickerRepository
    ) {
        this.marketDataRepository = marketDataRepository;
        this.renkoDataRepository = renkoDataRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public void calculate() {
        log.info("Starting Renko Data Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Renko Data for {}", ticker.getTickerSymbol());

            List<MarketData> allSeries = marketDataRepository.findByTickerOrderByMarketDataDateAsc(ticker);

            if (allSeries.isEmpty()) {
                log.error("No market data found for {}", ticker.getTickerSymbol());
                return;
            }

            List<RenkoData> renkoBricks = RenkoBricksGenerator.generateRenkoBricks(ticker, allSeries);
            if (!renkoBricks.isEmpty()) {
                renkoDataRepository.deleteByTicker(ticker);
                renkoDataRepository.flush();
                renkoDataRepository.saveAll(renkoBricks);
                log.info("Generated {} Renko bricks for {}", renkoBricks.size(), ticker.getTickerSymbol());
            }
        });

        log.info("Completed Renko Data Computation");
    }
}
