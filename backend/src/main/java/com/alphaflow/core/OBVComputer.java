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
import java.util.List;
import java.util.Optional;

import static com.alphaflow.domain.enums.TransformationType.NONE;
import static com.alphaflow.domain.enums.WindowPeriod.ZERO;
import static com.alphaflow.infrastructure.config.Constants.DB_MATH_CONTEXT;
import static java.time.LocalDate.EPOCH;

@Service
public class OBVComputer {

    private static final Logger log = LoggerFactory.getLogger(OBVComputer.class);

    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final TickerRepository tickerRepository;

    public OBVComputer(
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
            log.info("Computing OBV for {}", ticker.getTickerSymbol());

            List<MarketData> allSeries = marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                    ticker, EPOCH);

            if (allSeries.isEmpty()) {
                log.warn("No market data found for {}", ticker.getTickerSymbol());
                return;
            }

            computeOBV(ticker, allSeries);
        });

        log.info("Completed OBV Computation");
    }

    private void computeOBV(Ticker ticker, List<MarketData> allSeries) {
        Optional<MarketState> latestObv = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, MarketDataMetricType.OBV.code(), NONE.code(), ZERO.days()
        );

        BigDecimal currentObv;
        int startIndex;

        if (latestObv.isPresent()) {
            currentObv = latestObv.get().getValue();
            LocalDate lastDate = latestObv.get().getMarketStateDate();
            startIndex = findIndexForDate(allSeries, lastDate) + 1;
            if (startIndex <= 0 || startIndex >= allSeries.size()) {
                return;
            }
        } else {
            // First record: OBV = first day's volume
            currentObv = allSeries.getFirst().getVolume();
            persist(allSeries.getFirst(), MarketDataMetricType.OBV, NONE, ZERO.days(), currentObv);
            startIndex = 1;
        }

        int count = 0;
        for (int i = startIndex; i < allSeries.size(); i++) {
            MarketData current = allSeries.get(i);
            MarketData previous = allSeries.get(i - 1);

            int cmp = current.getPriceClose().compareTo(previous.getPriceClose());
            if (cmp > 0) {
                currentObv = currentObv.add(current.getVolume(), DB_MATH_CONTEXT);
            } else if (cmp < 0) {
                currentObv = currentObv.subtract(current.getVolume(), DB_MATH_CONTEXT);
            }

            persist(current, MarketDataMetricType.OBV, NONE, ZERO.days(), currentObv);
            count++;
        }
        log.info("Computed {} new OBV records for {}", count, ticker.getTickerSymbol());
    }

    private int findIndexForDate(List<MarketData> allSeries, LocalDate date) {
        for (int i = 0; i < allSeries.size(); i++) {
            if (allSeries.get(i).getMarketDataDate().isEqual(date)) {
                return i;
            }
        }
        return -1;
    }

    private void persist(MarketData marketData, MarketDataMetricType metric, TransformationType maType, int period, BigDecimal value) {
        MarketState marketState = marketStateRepository.findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(
                        marketData.getTicker(),
                        marketData.getMarketDataDate(),
                        metric.code(),
                        maType.code(),
                        period)
                .orElseGet(MarketState::new);

        marketState.setTicker(marketData.getTicker());
        marketState.setMarketStateDate(marketData.getMarketDataDate());
        marketState.setMetric(metric.code());
        marketState.setMaType(maType.code());
        marketState.setPeriod(period);
        marketState.setValue(value);

        marketStateRepository.save(marketState);
    }
}
