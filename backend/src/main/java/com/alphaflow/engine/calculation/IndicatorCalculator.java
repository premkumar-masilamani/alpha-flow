package com.alphaflow.engine.calculation;

import com.alphaflow.engine.enums.CandleDataMetricType;
import com.alphaflow.engine.enums.TransformationType;
import com.alphaflow.engine.enums.WindowPeriod;
import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.entities.Indicator;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.repositories.CandleDataRepository;
import com.alphaflow.infrastructure.repositories.IndicatorRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.alphaflow.engine.enums.TransformationType.*;
import static com.alphaflow.engine.enums.WindowPeriod.ZERO_DAYS;
import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;
import static java.math.BigDecimal.valueOf;

@Service
public class IndicatorCalculator {

    private static final Logger log = LoggerFactory.getLogger(IndicatorCalculator.class);

    private final CandleDataRepository candleDataRepository;
    private final IndicatorRepository indicatorRepository;
    private final TickerRepository tickerRepository;

    public IndicatorCalculator(
            CandleDataRepository candleDataRepository,
            IndicatorRepository indicatorRepository,
            TickerRepository tickerRepository
    ) {
        this.candleDataRepository = candleDataRepository;
        this.indicatorRepository = indicatorRepository;
        this.tickerRepository = tickerRepository;
    }

    public void calculate() {
        log.info("Starting Indicator Computation");

        tickerRepository.findByIsActiveTrue().forEach(this::calculateForTicker);

        log.info("Completed Indicator Computation");
    }

    private void calculateForTicker(Ticker ticker) {
        log.info("Computing Indicators for {}", ticker.getTickerSymbol());

        List<CandleData> allSeries = candleDataRepository.findByTickerOrderByCandleDataDateAsc(ticker);
        if (allSeries.isEmpty()) {
            log.warn("No candles found for {}", ticker.getTickerSymbol());
            return;
        }

        for (CandleDataMetricType metric : CandleDataMetricType.values()) {
            calculateMetric(ticker, metric, allSeries);
        }
    }

    private void calculateMetric(Ticker ticker, CandleDataMetricType metric, List<CandleData> allSeries) {
        if (metric.transformSpecs().isEmpty()) {
            return;
        }

        if (metric == CandleDataMetricType.OBV) {
            computeOBV(ticker, metric, allSeries);
            return;
        }

        for (var spec : metric.transformSpecs()) {
            for (TransformationType transformation : spec.transformations()) {
                for (WindowPeriod period : spec.periods()) {
                    computeTransformation(ticker, metric, transformation, period, allSeries);
                }
            }
        }
    }

    private void computeTransformation(Ticker ticker, CandleDataMetricType metric, TransformationType transformation, WindowPeriod period, List<CandleData> allSeries) {
        switch (transformation) {
            case SMA -> computeSMA(ticker, metric, period, allSeries);
            case EMA -> computeEMA(ticker, metric, period, allSeries);
        }
    }

    private void computeOBV(Ticker ticker, CandleDataMetricType metric, List<CandleData> allSeries) {
        if (allSeries.isEmpty()) {
            return;
        }

        Optional<Indicator> latestIndicator = indicatorRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByIndicatorDateDesc(
                ticker, metric.code(), OBV.code(), ZERO_DAYS.days()
        );

        BigDecimal onBalanceVolume;
        int startIndex;

        if (latestIndicator.isPresent()) {
            Indicator lastOnBalanceVolume = latestIndicator.get();
            onBalanceVolume = lastOnBalanceVolume.getValue();
            startIndex = findIndexForDate(allSeries, lastOnBalanceVolume.getIndicatorDate()) + 1;
            if (startIndex <= 0 || startIndex >= allSeries.size()) {
                log.debug("OBV is up to date for {}", ticker.getTickerSymbol());
                return;
            }
        } else {
            // First day's OBV is 0
            persist(allSeries.getFirst(), metric, OBV, ZERO_DAYS.days(), BigDecimal.ZERO);
            onBalanceVolume = BigDecimal.ZERO;
            startIndex = 1;
        }

        for (int i = startIndex; i < allSeries.size(); i++) {
            CandleData currentData = allSeries.get(i);
            CandleData previousData = allSeries.get(i - 1);

            int priceCompare = currentData.getPriceClose().compareTo(previousData.getPriceClose());

            if (priceCompare > 0) {
                onBalanceVolume = onBalanceVolume.add(currentData.getVolume());
            } else if (priceCompare < 0) {
                onBalanceVolume = onBalanceVolume.subtract(currentData.getVolume());
            }
            // If prices are equal, OBV is unchanged

            persist(currentData, metric, OBV, ZERO_DAYS.days(), onBalanceVolume);
        }
        log.info("Computed OBV for {}", ticker.getTickerSymbol());
    }

    /**
     * Simple Moving Average (SMA) calculation.
     * Uses a sliding window approach for O(N) efficiency.
     * Formula: SMA = (Sum of values in window) / Period
     */
    private void computeSMA(Ticker ticker, CandleDataMetricType metric, WindowPeriod maPeriod, List<CandleData> allSeries) {
        int period = maPeriod.days();
        if (allSeries.size() < period) return;

        // Find the latest SMA already in the database to resume computation
        int startIndex = getStartIndex(ticker, allSeries, metric, SMA, period);
        if (startIndex < 0 || startIndex >= allSeries.size()) {
            return;
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
            log.debug("{} {} {} SMA: {}", ticker.getTickerSymbol(), metric.code(), allSeries.get(i).getCandleDataDate(), sma);

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
    private void computeEMA(Ticker ticker, CandleDataMetricType metric, WindowPeriod maPeriod, List<CandleData> allSeries) {
        int period = maPeriod.days();
        if (allSeries.size() < period) return;

        // Find the latest EMA already in the database to resume computation
        int startIndex = getStartIndex(ticker, allSeries, metric, EMA, period);
        if (startIndex < 0 || startIndex >= allSeries.size()) {
            return;
        }

        BigDecimal ema;
        BigDecimal alpha = valueOf(2).divide(valueOf(period + 1), DB_MATH_CONTEXT);

        Optional<Indicator> latestEma = indicatorRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByIndicatorDateDesc(
                ticker, metric.code(), EMA.code(), period
        );

        if (latestEma.isPresent() && findIndexForDate(allSeries, latestEma.get().getIndicatorDate()) + 1 == startIndex) {
            ema = latestEma.get().getValue();
        } else {
            // Seed EMA with SMA of the first 'period' elements
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < period; i++) {
                sum = sum.add(metric.extract(allSeries.get(i)), DB_MATH_CONTEXT);
            }
            ema = sum.divide(valueOf(period), DB_MATH_CONTEXT);
            persist(allSeries.get(period - 1), metric, EMA, period, ema);
            log.debug("{} {} {} Seed EMA: {}", ticker.getTickerSymbol(), metric.code(), allSeries.get(period - 1).getCandleDataDate(), ema);
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
            log.debug("{} {} {} EMA: {}", ticker.getTickerSymbol(), metric.code(), allSeries.get(i).getCandleDataDate(), ema);
            count++;
        }
        log.info("Computed {} new EMA records for {} - {} ({} days)", count, ticker.getTickerSymbol(), metric.code(), period);
    }

    private int getStartIndex(Ticker ticker, List<CandleData> allSeries, CandleDataMetricType metric, TransformationType maType, int period) {
        Optional<Indicator> latestIndicator = indicatorRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByIndicatorDateDesc(
                ticker, metric.code(), maType.code(), period
        );

        if (latestIndicator.isPresent()) {
            LocalDate lastDate = latestIndicator.get().getIndicatorDate();
            return findIndexForDate(allSeries, lastDate) + 1;
        } else {
            return period - 1;
        }
    }

    private int findIndexForDate(List<CandleData> allSeries, LocalDate date) {
        int low = 0;
        int high = allSeries.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            LocalDate midDate = allSeries.get(mid).getCandleDataDate();
            int cmp = midDate.compareTo(date);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    private void persist(CandleData candleData, CandleDataMetricType metric, TransformationType maType, int period, BigDecimal value) {
        // We use findBy... to ensure idempotency and avoid duplicates if the computation is re-run for same dates
        Indicator indicator = indicatorRepository.findByTickerAndIndicatorDateAndMetricAndMaTypeAndPeriod(
                        candleData.getTicker(),
                        candleData.getCandleDataDate(),
                        metric.code(),
                        maType.code(),
                        period)
                .orElseGet(Indicator::new);

        indicator.setTicker(candleData.getTicker());
        indicator.setIndicatorDate(candleData.getCandleDataDate());
        indicator.setMetric(metric.code());
        indicator.setMaType(maType.code());
        indicator.setPeriod(period);
        indicator.setValue(value);

        indicatorRepository.save(indicator);
    }
}
