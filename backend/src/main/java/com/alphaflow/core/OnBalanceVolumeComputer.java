package com.alphaflow.core;

import com.alphaflow.domain.enums.MarketDataMetricType;
import com.alphaflow.domain.enums.TransformationType;
import com.alphaflow.domain.enums.WindowPeriod;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.EPOCH;

@Service
public class OnBalanceVolumeComputer {

    private static final Logger log = LoggerFactory.getLogger(OnBalanceVolumeComputer.class);

    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final TickerRepository tickerRepository;

    public OnBalanceVolumeComputer(
        MarketDataRepository marketDataRepository,
        MarketStateRepository marketStateRepository,
        TickerRepository tickerRepository
    ) {
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.tickerRepository = tickerRepository;
    }

    public void compute() {
        log.info("Starting On-Balance Volume (OBV) Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Calculating OBV for {}", ticker.getTickerSymbol());

            List<MarketData> allSeries = marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                ticker, EPOCH
            );

            if (allSeries.isEmpty()) {
                log.warn("No market data found for {}, skipping OBV.", ticker.getTickerSymbol());
                return;
            }

            Optional<MarketState> latestObv = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, MarketDataMetricType.OBV.code(), TransformationType.NONE.code(), WindowPeriod.NONE.days()
            );

            BigDecimal obv;
            int startIndex;
            List<MarketState> toSave = new ArrayList<>();

            if (latestObv.isPresent()) {
                MarketState lastObv = latestObv.get();
                obv = lastObv.getValue();
                startIndex = findIndexForDate(allSeries, lastObv.getMarketStateDate()) + 1;
                if (startIndex <= 0 || startIndex >= allSeries.size()) {
                    log.debug("OBV is up to date for {}", ticker.getTickerSymbol());
                    return;
                }
            } else {
                // First day's OBV is 0
                toSave.add(createMarketState(allSeries.get(0), BigDecimal.ZERO));
                obv = BigDecimal.ZERO;
                startIndex = 1;
            }

            for (int i = startIndex; i < allSeries.size(); i++) {
                MarketData currentData = allSeries.get(i);
                MarketData previousData = allSeries.get(i - 1);

                int priceCompare = currentData.getPriceClose().compareTo(previousData.getPriceClose());

                if (priceCompare > 0) {
                    obv = obv.add(currentData.getVolume());
                } else if (priceCompare < 0) {
                    obv = obv.subtract(currentData.getVolume());
                }
                // If prices are equal, OBV is unchanged

                toSave.add(createMarketState(currentData, obv));
            }

            if (!toSave.isEmpty()) {
                log.info("Adding {} new OBV records for {}", toSave.size(), ticker.getTickerSymbol());
                marketStateRepository.saveAll(toSave);
            } else {
                log.debug("No new OBV records to add for {}", ticker.getTickerSymbol());
            }
        });

        log.info("Completed On-Balance Volume (OBV) Computation");
    }

    private int findIndexForDate(List<MarketData> allSeries, LocalDate date) {
        for (int i = 0; i < allSeries.size(); i++) {
            if (allSeries.get(i).getMarketDataDate().isEqual(date)) {
                return i;
            }
        }
        return -1;
    }

    private MarketState createMarketState(MarketData marketData, BigDecimal value) {
        MarketState state = new MarketState();
        state.setTicker(marketData.getTicker());
        state.setMarketStateDate(marketData.getMarketDataDate());
        state.setMetric(MarketDataMetricType.OBV.code());
        state.setMaType(TransformationType.NONE.code());
        state.setPeriod(WindowPeriod.NONE.days());
        state.setValue(value);
        return state;
    }
}
