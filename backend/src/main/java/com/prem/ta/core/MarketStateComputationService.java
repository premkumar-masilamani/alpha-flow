package com.prem.ta.core;

import com.prem.ta.entities.MarketData;
import com.prem.ta.entities.MarketState;
import com.prem.ta.entities.Ticker;
import com.prem.ta.repositories.MarketDataRepository;
import com.prem.ta.repositories.MarketStateRepository;
import com.prem.ta.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarketStateComputationService {

    private static final Logger log =
            LoggerFactory.getLogger(MarketStateComputationService.class);

    private static final MathContext MC = new MathContext(18);

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

        tickerRepository.findAll()
                .forEach(ticker -> {
                    log.info("Computing Market State for {}", ticker.getTickerSymbol());
//                    computeEMA(ticker, "VWAP", 8);
//                    computeEMA(ticker, "VALUE_RANGE", 5);
//                    computeEMA(ticker, "BUYER_CAPITAL", 8);

                    computeSMA(ticker, "VOLUME", 20);
//                    computeSMA(ticker, "POC", 10);
                });

        log.info("Market State computation complete");
    }

    /* ======================= EMA ======================= */

    private void computeEMA(
            Ticker ticker,
            String metric,
            int period
    ) {
        LocalDate startDate = resolveStartDate(ticker, metric, "EMA", period);
        if (startDate == null) return;

        List<MarketData> series =
                marketDataRepository
                        .findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                                ticker,
                                startDate
                        );

        if (series.size() < period) {
            for (MarketData md : series) {
                persist(md, metric, "EMA", period, BigDecimal.ZERO);
            }
            return;
        }

        List<BigDecimal> seedWindow = new ArrayList<>();
        BigDecimal ema = null;

        BigDecimal alpha =
                BigDecimal.valueOf(2)
                        .divide(BigDecimal.valueOf(period + 1), MC);

        for (MarketData md : series) {
            BigDecimal value = extractMetric(md, metric);
            if (value == null) continue;

            if (ema == null) {
                seedWindow.add(value);
                if (seedWindow.size() == period) {
                    ema = average(seedWindow);
                    persist(md, metric, "EMA", period, ema);
                }
            } else {
                ema = value.subtract(ema)
                        .multiply(alpha, MC)
                        .add(ema);
                persist(md, metric, "EMA", period, ema);
            }
        }
    }

    /* ======================= SMA ======================= */

    private void computeSMA(
            Ticker ticker,
            String metric,
            int period
    ) {
        LocalDate startDate = resolveStartDate(ticker, metric, "SMA", period);
        if (startDate == null) return;
        List<MarketData> series =
                marketDataRepository
                        .findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                                ticker,
                                startDate
                        );

        if (series.size() < period) {
            for (int i = period - 1; i < series.size(); i++) {
                persist(
                        series.get(i),
                        metric,
                        "SMA",
                        period,
                        BigDecimal.ZERO
                );
            }
            return;
        }
        ;

        for (int i = period - 1; i < series.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;

            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal v = extractMetric(series.get(j), metric);
                if (v == null) {
                    sum = null;
                    break;
                }
                sum = sum.add(v);
            }

            if (sum == null) continue;

            BigDecimal sma =
                    sum.divide(BigDecimal.valueOf(period), MC);

            persist(
                    series.get(i),
                    metric,
                    "SMA",
                    period,
                    sma
            );
        }
    }

    /* ======================= Helpers ======================= */

    private LocalDate resolveStartDate(
            Ticker ticker,
            String metric,
            String maType,
            int period
    ) {
        return marketStateRepository
                .findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                        ticker, metric, maType, period
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

    private BigDecimal extractMetric(MarketData md, String metric) {
        return switch (metric) {
            case "VWAP" -> md.getVwap();
            case "VOLUME" -> md.getVolume();
            case "POC" -> md.getVolumeProfilePOC();
            case "BUYER_CAPITAL" -> md.getBuyerCapitalShare();
            case "VALUE_RANGE" -> md.getVolumeProfileVAH()
                    .subtract(md.getVolumeProfileVAL());
            default -> null;
        };
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MC);
    }

    private void persist(
            MarketData md,
            String metric,
            String maType,
            int period,
            BigDecimal value
    ) {
        log.debug("Date={}, Metric={}, MaType={}, Period={}, Value={}", md.getMarketDataDate(), metric, maType, period, value);
        marketStateRepository
                .findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(
                        md.getTicker(),
                        md.getMarketDataDate(),
                        metric,
                        maType,
                        period
                )
                .ifPresentOrElse(existing -> {
                    existing.setValue(value);
                    marketStateRepository.save(existing);
                }, () -> {
                    MarketState ms = new MarketState();
                    ms.setTicker(md.getTicker());
                    ms.setMarketStateDate(md.getMarketDataDate());
                    ms.setMetric(metric);
                    ms.setMaType(maType);
                    ms.setPeriod(period);
                    ms.setValue(value);
                    marketStateRepository.save(ms);
                });
    }
}
