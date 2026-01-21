package com.prem.ta.core;

import com.prem.ta.entities.MarketData;
import com.prem.ta.entities.MarketState;
import com.prem.ta.entities.Ticker;
import com.prem.ta.models.MarketStateMetricType;
import com.prem.ta.models.MovingAveragePeriod;
import com.prem.ta.models.MovingAverageType;
import com.prem.ta.repositories.MarketDataRepository;
import com.prem.ta.repositories.MarketStateRepository;
import com.prem.ta.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.prem.ta.configs.Constants.DB_MATH_CONTEXT;
import static com.prem.ta.models.MovingAverageType.EMA;
import static com.prem.ta.models.MovingAverageType.SMA;
import static java.math.BigDecimal.valueOf;

@Service
public class MarketStateComputationService {

    private static final Logger log = LoggerFactory.getLogger(MarketStateComputationService.class);

    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final TickerRepository tickerRepository;

    public MarketStateComputationService(
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            TickerRepository tickerRepository
    ) {
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.tickerRepository = tickerRepository;
    }

    public void compute() {
        log.info("Computing Market State");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Market State for {}", ticker.getTickerSymbol());

            // 1. Fetch all available market data for the ticker, sorted by date
            List<MarketData> allSeries = marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                    ticker, LocalDate.of(1900, 1, 1));

            if (allSeries.isEmpty()) {
                return;
            }

            for (MarketStateMetricType metric : MarketStateMetricType.values()) {
                for (MovingAveragePeriod maPeriod : MovingAveragePeriod.values()) {
                    computeSMA(ticker, metric, maPeriod, allSeries);
                    computeEMA(ticker, metric, maPeriod, allSeries);
                }
            }
        });

        log.info("Market State computation complete");
    }

    private void computeSMA(Ticker ticker, MarketStateMetricType metric, MovingAveragePeriod maPeriod, List<MarketData> allSeries) {
        int period = maPeriod.days();
        if (allSeries.size() < period) return;

        // Find the latest SMA already in the database
        Optional<MarketState> latestSma = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, metric.code(), SMA.code(), period
        );

        int startIndex;
        if (latestSma.isPresent()) {
            LocalDate lastDate = latestSma.get().getMarketStateDate();
            startIndex = findIndexForDate(allSeries, lastDate) + 1;
            if (startIndex <= 0 || startIndex >= allSeries.size()) return;
        } else {
            startIndex = period - 1;
        }

        // Using sliding window for SMA
        BigDecimal sum = BigDecimal.ZERO;
        // Initial sum for the window ending just before startIndex
        for (int j = startIndex - period + 1; j < startIndex; j++) {
            sum = sum.add(metric.extract(allSeries.get(j)), DB_MATH_CONTEXT);
        }

        for (int i = startIndex; i < allSeries.size(); i++) {
            sum = sum.add(metric.extract(allSeries.get(i)), DB_MATH_CONTEXT);
            BigDecimal sma = sum.divide(valueOf(period), DB_MATH_CONTEXT);
            persist(allSeries.get(i), metric, SMA, period, sma);
            // Sliding the window: subtract the element that will be out of window in next iteration
            sum = sum.subtract(metric.extract(allSeries.get(i - period + 1)), DB_MATH_CONTEXT);
        }
    }

    private void computeEMA(Ticker ticker, MarketStateMetricType metric, MovingAveragePeriod maPeriod, List<MarketData> allSeries) {
        int period = maPeriod.days();
        if (allSeries.size() < period) return;

        // Find the latest EMA already in the database
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
            if (startIndex <= 0 || startIndex >= allSeries.size()) return;
        } else {
            // Seed EMA with SMA of the first 'period' elements
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < period; i++) {
                sum = sum.add(metric.extract(allSeries.get(i)), DB_MATH_CONTEXT);
            }
            ema = sum.divide(valueOf(period), DB_MATH_CONTEXT);
            persist(allSeries.get(period - 1), metric, EMA, period, ema);
            startIndex = period;
        }

        // EMAₜ = EMAₜ₋₁ + α × (valueₜ − EMAₜ₋₁)
        for (int i = startIndex; i < allSeries.size(); i++) {
            BigDecimal value = metric.extract(allSeries.get(i));
            ema = value.subtract(ema, DB_MATH_CONTEXT)
                    .multiply(alpha, DB_MATH_CONTEXT)
                    .add(ema, DB_MATH_CONTEXT);
            persist(allSeries.get(i), metric, EMA, period, ema);
        }
    }

    private int findIndexForDate(List<MarketData> allSeries, LocalDate date) {
        for (int i = 0; i < allSeries.size(); i++) {
            if (allSeries.get(i).getMarketDataDate().isEqual(date)) {
                return i;
            }
        }
        return -1;
    }

    private void persist(MarketData marketData, MarketStateMetricType metric, MovingAverageType maType, int period, BigDecimal value) {
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
