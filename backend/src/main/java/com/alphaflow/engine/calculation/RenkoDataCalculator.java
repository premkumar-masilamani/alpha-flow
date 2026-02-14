package com.alphaflow.engine.calculation;

import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.generators.RenkoDataGenerator;
import com.alphaflow.infrastructure.repositories.CandleDataRepository;
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

    private final CandleDataRepository candleDataRepository;
    private final RenkoDataRepository renkoDataRepository;
    private final TickerRepository tickerRepository;

    public RenkoDataCalculator(
            CandleDataRepository candleDataRepository,
            RenkoDataRepository renkoDataRepository,
            TickerRepository tickerRepository
    ) {
        this.candleDataRepository = candleDataRepository;
        this.renkoDataRepository = renkoDataRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public void calculate() {
        log.info("Starting Renko data computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Renko data for {}", ticker.getTickerSymbol());

            List<CandleData> allSeries = candleDataRepository.findByTickerOrderByCandleDataDateAsc(ticker);

            if (allSeries.isEmpty()) {
                log.error("No candle data found for {}", ticker.getTickerSymbol());
                return;
            }

            List<RenkoData> renkoData = RenkoDataGenerator.generateRenkoData(ticker, allSeries);
            if (!renkoData.isEmpty()) {
                renkoDataRepository.deleteByTicker(ticker);
                renkoDataRepository.flush();
                renkoDataRepository.saveAll(renkoData);
                log.info("Generated {} Renko data for {}", renkoData.size(), ticker.getTickerSymbol());
            }
        });

        log.info("Completed Renko data computation");
    }
}
