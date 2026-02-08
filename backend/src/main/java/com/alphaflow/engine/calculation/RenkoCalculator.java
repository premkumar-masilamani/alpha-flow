package com.alphaflow.engine.calculation;

import com.alphaflow.infrastructure.entities.Candle;
import com.alphaflow.infrastructure.entities.Renko;
import com.alphaflow.infrastructure.generators.RenkoBricksGenerator;
import com.alphaflow.infrastructure.repositories.CandleRepository;
import com.alphaflow.infrastructure.repositories.RenkoRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RenkoCalculator {

    private static final Logger log = LoggerFactory.getLogger(RenkoCalculator.class);

    private final CandleRepository candleRepository;
    private final RenkoRepository renkoRepository;
    private final TickerRepository tickerRepository;

    public RenkoCalculator(
            CandleRepository candleRepository,
            RenkoRepository renkoRepository,
            TickerRepository tickerRepository
    ) {
        this.candleRepository = candleRepository;
        this.renkoRepository = renkoRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public void calculate() {
        log.info("Starting Renko Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Renko for {}", ticker.getTickerSymbol());

            List<Candle> allSeries = candleRepository.findByTickerOrderByCandleDateAsc(ticker);

            if (allSeries.isEmpty()) {
                log.error("No candles found for {}", ticker.getTickerSymbol());
                return;
            }

            List<Renko> renkoBricks = RenkoBricksGenerator.generateRenkoBricks(ticker, allSeries);
            if (!renkoBricks.isEmpty()) {
                renkoRepository.deleteByTicker(ticker);
                renkoRepository.flush();
                renkoRepository.saveAll(renkoBricks);
                log.info("Generated {} Renko bricks for {}", renkoBricks.size(), ticker.getTickerSymbol());
            }
        });

        log.info("Completed Renko Computation");
    }
}
