package com.alphaflow.engine.calculation;

import com.alphaflow.infrastructure.entities.CandleBar;
import com.alphaflow.infrastructure.entities.RenkoBrick;
import com.alphaflow.infrastructure.generators.RenkoBricksGenerator;
import com.alphaflow.infrastructure.repositories.CandleBarRepository;
import com.alphaflow.infrastructure.repositories.RenkoBrickRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RenkoBrickCalculator {

    private static final Logger log = LoggerFactory.getLogger(RenkoBrickCalculator.class);

    private final CandleBarRepository candleBarRepository;
    private final RenkoBrickRepository renkoBrickRepository;
    private final TickerRepository tickerRepository;

    public RenkoBrickCalculator(
            CandleBarRepository candleBarRepository,
            RenkoBrickRepository renkoBrickRepository,
            TickerRepository tickerRepository
    ) {
        this.candleBarRepository = candleBarRepository;
        this.renkoBrickRepository = renkoBrickRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public void calculate() {
        log.info("Starting Renko Brick Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Renko bricks for {}", ticker.getTickerSymbol());

            List<CandleBar> allSeries = candleBarRepository.findByTickerOrderByCandleBarDateAsc(ticker);

            if (allSeries.isEmpty()) {
                log.error("No candle bars found for {}", ticker.getTickerSymbol());
                return;
            }

            List<RenkoBrick> renkoBricks = RenkoBricksGenerator.generateRenkoBricks(ticker, allSeries);
            if (!renkoBricks.isEmpty()) {
                renkoBrickRepository.deleteByTicker(ticker);
                renkoBrickRepository.flush();
                renkoBrickRepository.saveAll(renkoBricks);
                log.info("Generated {} Renko bricks for {}", renkoBricks.size(), ticker.getTickerSymbol());
            }
        });

        log.info("Completed Renko Brick Computation");
    }
}
