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

import static com.alphaflow.domain.enums.TransformationType.EMA;
import static com.alphaflow.domain.enums.TransformationType.SMA;
import static com.alphaflow.infrastructure.config.Constants.DB_MATH_CONTEXT;
import static java.math.BigDecimal.valueOf;
import static java.time.LocalDate.EPOCH;

@Service
public class MarketStateComputer {

    private static final Logger log = LoggerFactory.getLogger(MarketStateComputer.class);

    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final TickerRepository tickerRepository;

    public MarketStateComputer(
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            TickerRepository tickerRepository
    ) {
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.tickerRepository = tickerRepository;
    }

    public void compute() {
        log.info("Starting Market State Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Market State for {}", ticker.getTickerSymbol());

            // 1. Fetch all available market data for the ticker, sorted by date
            // We fetch everything once to avoid N+1 query problems and redundant DB round-trips
            List<MarketData> allSeries = marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                    ticker, EPOCH);

            if (allSeries.isEmpty()) {
                log.warn("No market data found for {}", ticker.getTickerSymbol());
                return;
            }

            for (MarketDataMetricType metric : MarketDataMetricType.values()) {
                var spec = metric.transformSpec();
                for (TransformationType transformation : spec.transformations()) {
                    for (WindowPeriod period : spec.periods()) {
                        switch (transformation) {
                            case SMA -> computeSMA(ticker, metric, period, allSeries);
                            case EMA -> computeEMA(ticker, metric, period, allSeries);
                        }
                    }
                }
            }
        });

        log.info("Completed Market State Computation");
    }

    /**
     * Simple Moving Average (SMA) calculation.
     * Uses a sliding window approach for O(N) efficiency.
     * Formula: SMA = (Sum of values in window) / Period
     */
    private void computeSMA(Ticker ticker, MarketDataMetricType metric, WindowPeriod maPeriod, List<MarketData> allSeries) {
        int period = maPeriod.days();
        if (allSeries.size() < period) return;

        // Find the latest SMA already in the database to resume computation
        Optional<MarketState> latestSma = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, metric.code(), SMA.code(), period
        );

        int startIndex;
        if (latestSma.isPresent()) {
            LocalDate lastDate = latestSma.get().getMarketStateDate();
            startIndex = findIndexForDate(allSeries, lastDate) + 1;
            // If up to date, skip
            if (startIndex <= 0 || startIndex >= allSeries.size()) {
                return;
            }
        } else {
            // First ever computation starts at the first possible date where a full window exists
            startIndex = period - 1;
        }

        // Initialize sliding window sum
        BigDecimal sum = BigDecimal.ZERO;
        // Sum values for the window ending just before our starting point
        for (int j = startIndex - period + 1; j < startIndex; j++) {
            sum = sum.add(metric.extract(allSeries.get(j)), DB_MATH_CONTEXT);
        }

        int count = 0;
        for (int i = startIndex; i < allSeries.size(); i++) {
            // Add current value to window sum
            sum = sum.add(metric.extract(allSeries.get(i)), DB_MATH_CONTEXT);

            BigDecimal sma = sum.divide(valueOf(period), DB_MATH_CONTEXT);
            persist(allSeries.get(i), metric, SMA, period, sma);
            log.debug("{} {} {} SMA: {}", ticker.getTickerSymbol(), metric.code(), allSeries.get(i).getMarketDataDate(), sma);

            // Slide window: subtract the oldest value (which will be out of window in next step)
            sum = sum.subtract(metric.extract(allSeries.get(i - period + 1)), DB_MATH_CONTEXT);
            count++;
        }
        log.info("Computed {} new SMA records for {} - {} ({} days)", count, ticker.getTickerSymbol(), metric.code(), period);
    }

    /**
     * Exponential Moving Average (EMA) calculation.
     * Formula: EMAₜ = EMAₜ₋₁ + α × (Valueₜ − EMAₜ₋₁)
     * where α = 2 / (Period + 1)
     * Initial Seed: The first EMA value is typically the SMA of the first 'Period' days.
     */
    private void computeEMA(Ticker ticker, MarketDataMetricType metric, WindowPeriod maPeriod, List<MarketData> allSeries) {
        int period = maPeriod.days();
        if (allSeries.size() < period) return;

        // Find the latest EMA already in the database to resume computation
        Optional<MarketState> latestEma = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, metric.code(), EMA.code(), period
        );

        BigDecimal ema;
        int startIndex;
        BigDecimal alpha = valueOf(2).divide(valueOf(period + 1), DB_MATH_CONTEXT);

        if (latestEma.isPresent()) {
            ema = latestEma.get().getValue();
            LocalDate lastDate = latestEma.get().getMarketStateDate();
            startIndex = findIndexForDate(allSeries, lastDate) + 1;
            // If up to date, skip
            if (startIndex <= 0 || startIndex >= allSeries.size()) {
                return;
            }
        } else {
            // Seed EMA with SMA of the first 'period' elements
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < period; i++) {
                sum = sum.add(metric.extract(allSeries.get(i)), DB_MATH_CONTEXT);
            }
            ema = sum.divide(valueOf(period), DB_MATH_CONTEXT);
            persist(allSeries.get(period - 1), metric, EMA, period, ema);
            log.debug("{} {} {} Seed EMA: {}", ticker.getTickerSymbol(), metric.code(), allSeries.get(period - 1).getMarketDataDate(), ema);
            startIndex = period;
        }

        // Apply EMA recursive formula for remaining dates
        int count = 0;
        for (int i = startIndex; i < allSeries.size(); i++) {
            BigDecimal value = metric.extract(allSeries.get(i));

            // EMAₜ = EMAₜ₋₁ + α × (valueₜ − EMAₜ₋₁)
            ema = value.subtract(ema, DB_MATH_CONTEXT)
                    .multiply(alpha, DB_MATH_CONTEXT)
                    .add(ema, DB_MATH_CONTEXT);

            persist(allSeries.get(i), metric, EMA, period, ema);
            log.debug("{} {} {} EMA: {}", ticker.getTickerSymbol(), metric.code(), allSeries.get(i).getMarketDataDate(), ema);
            count++;
        }
        log.info("Computed {} new EMA records for {} - {} ({} days)", count, ticker.getTickerSymbol(), metric.code(), period);
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
        // We use findBy... to ensure idempotency and avoid duplicates if the computation is re-run for same dates
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
