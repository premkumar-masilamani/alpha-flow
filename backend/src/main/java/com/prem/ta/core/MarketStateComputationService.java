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
import static java.math.BigDecimal.valueOf;

@Service
public class MarketStateComputationService {

    private static final Logger log =
            LoggerFactory.getLogger(MarketStateComputationService.class);

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

        tickerRepository.findAll().forEach(ticker -> {
            log.info("Computing Market State for {}", ticker.getTickerSymbol());

            for (MarketStateMetricType metric : MarketStateMetricType.values()) {
                for (MovingAverageType maType : MovingAverageType.values()) {
                    for (MovingAveragePeriod maPeriod : MovingAveragePeriod.values()) {
                        computeMA(ticker, metric, maType, maPeriod);
                    }
                }
            }
        });

        log.info("Market State computation complete");
    }

    private void computeMA(
            Ticker ticker,
            MarketStateMetricType metric,
            MovingAverageType maType,
            MovingAveragePeriod maPeriod
    ) {
        int period = maPeriod.days();

        LocalDate startDate = resolveStartDate(ticker, metric, maType, period);

        // No Market Data exists, nothing to compute
        if (startDate == null) return;

        // TODO: Stream the data, use windows, instead of keeping the records in the memory
        List<MarketData> series =
                marketDataRepository
                        .findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                                ticker,
                                startDate
                        );

        if (series.size() < period) return;

        switch (maType) {
            case SMA -> computeSMA(series, metric, period);
            case EMA -> computeEMA(series, metric, period);
        }
    }

    private void computeSMA(
            List<MarketData> series,
            MarketStateMetricType metric,
            int period
    ) {
        for (int i = period - 1; i < series.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal value = metric.extract(series.get(j));
                sum = sum.add(value, DB_MATH_CONTEXT);
            }
            BigDecimal sma = sum.divide(valueOf(period), DB_MATH_CONTEXT);

            log.debug("{} {} {} {}", metric, MovingAverageType.SMA, period, sma);
            persist(series.get(i), metric, MovingAverageType.SMA, period, sma);
        }
    }

    /**
     * EMAₜ = EMAₜ₋₁ + α × (valueₜ − EMAₜ₋₁)
     * α = 2 / (period + 1)
     */
    private void computeEMA(
            List<MarketData> series,
            MarketStateMetricType metric,
            int period
    ) {

        // α = 2 / (period + 1)
        BigDecimal alpha =
                valueOf(2)
                        .divide(valueOf(period + 1), DB_MATH_CONTEXT);

        // Fetch latest EMA from the database
        Optional<MarketState> lastEmaState = marketStateRepository
                .findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                        series.getFirst().getTicker(),
                        metric.code(),
                        MovingAverageType.EMA.code(),
                        period
                );

        // If not present, compute & persist seed EMA
        BigDecimal ema;
        int startIndex;
        if (lastEmaState.isPresent()) {
            ema = lastEmaState.get().getValue();
            startIndex = 0;
        } else {
            EMASeed seed = computeAndPersistFirstEMA(series, metric, period);
            if (seed == null) return;

            ema = seed.ema();
            startIndex = seed.nextIndex();
        }

        // Continue EMA calculation for rest of the series
        for (int i = startIndex; i < series.size(); i++) {
            MarketData marketData = series.get(i);
            BigDecimal value = metric.extract(marketData);

            // EMAₜ = EMAₜ₋₁ + α × (valueₜ − EMAₜ₋₁)
            ema = value.subtract(ema, DB_MATH_CONTEXT)
                    .multiply(alpha, DB_MATH_CONTEXT)
                    .add(ema, DB_MATH_CONTEXT);

            log.debug("{} {} {} {}", metric, MovingAverageType.EMA, period, ema);
            persist(
                    marketData,
                    metric,
                    MovingAverageType.EMA,
                    period,
                    ema
            );
        }
    }

    private EMASeed computeAndPersistFirstEMA(
            List<MarketData> series,
            MarketStateMetricType metric,
            int period
    ) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (int i = 0; i < series.size(); i++) {
            MarketData marketData = series.get(i);
            BigDecimal value = metric.extract(marketData);

            sum = sum.add(value, DB_MATH_CONTEXT);
            count++;
            if (count == period) {
                BigDecimal ema = sum.divide(valueOf(period), DB_MATH_CONTEXT);
                log.debug("{} {} {} {}", metric, MovingAverageType.EMA, period, ema);
                persist(
                        marketData,
                        metric,
                        MovingAverageType.EMA,
                        period,
                        ema
                );

                // Next EMA computation starts AFTER the seed day
                return new EMASeed(ema, i + 1);
            }
        }

        return null; // not enough data to seed EMA
    }


    private LocalDate resolveStartDate(
            Ticker ticker,
            MarketStateMetricType metric,
            MovingAverageType maType,
            int period
    ) {
        return marketStateRepository
                .findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                        ticker,
                        metric.code(),
                        maType.code(),
                        period
                )
                .map(ms -> ms.getMarketStateDate().minusDays(period - 1))
                .orElseGet(() -> earliestMarketDataDate(ticker));
    }

    private LocalDate earliestMarketDataDate(Ticker ticker) {
        return marketDataRepository
                .findFirstByTickerOrderByMarketDataDateAsc(ticker)
                .map(MarketData::getMarketDataDate)
                .orElse(null);
    }

    private void persist(
            MarketData marketData,
            MarketStateMetricType metric,
            MovingAverageType maType,
            int period,
            BigDecimal value
    ) {
        MarketState marketState =
                marketStateRepository
                        .findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(
                                marketData.getTicker(),
                                marketData.getMarketDataDate(),
                                metric.code(),
                                maType.code(),
                                period
                        )
                        .orElseGet(MarketState::new);

        marketState.setTicker(marketData.getTicker());
        marketState.setMarketStateDate(marketData.getMarketDataDate());
        marketState.setMetric(metric.code());
        marketState.setMaType(maType.code());
        marketState.setPeriod(period);
        marketState.setValue(value);

        marketStateRepository.save(marketState);
    }

    private record EMASeed(BigDecimal ema, int nextIndex) {
    }
}
