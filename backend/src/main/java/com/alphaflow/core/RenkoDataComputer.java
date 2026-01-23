package com.alphaflow.core;

import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.RenkoDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.alphaflow.infrastructure.config.Constants.DB_MATH_CONTEXT;
import static java.math.BigDecimal.valueOf;

@Service
public class RenkoDataComputer {

    private static final Logger log = LoggerFactory.getLogger(RenkoDataComputer.class);

    private static final int PERIOD_COUNT = 180;

    private final MarketDataRepository marketDataRepository;
    private final RenkoDataRepository renkoDataRepository;
    private final TickerRepository tickerRepository;

    public RenkoDataComputer(
            MarketDataRepository marketDataRepository,
            RenkoDataRepository renkoDataRepository,
            TickerRepository tickerRepository
    ) {
        this.marketDataRepository = marketDataRepository;
        this.renkoDataRepository = renkoDataRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public void compute() {
        log.info("Starting Renko Data Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Computing Renko Data for {}", ticker.getTickerSymbol());

            List<MarketData> allSeries = marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(
                    ticker, LocalDate.of(1900, 1, 1));

            if (allSeries.isEmpty()) {
                log.warn("No market data found for {}", ticker.getTickerSymbol());
                return;
            }

            // Clear old data
            renkoDataRepository.deleteByTicker(ticker);
            renkoDataRepository.flush();

            List<RenkoData> renkoBricks = generateRenkoBricks(ticker, allSeries);
            if (!renkoBricks.isEmpty()) {
                renkoDataRepository.saveAll(renkoBricks);
                log.info("Generated {} Renko bricks for {}", renkoBricks.size(), ticker.getTickerSymbol());
            } else {
                log.warn("No Renko bricks generated for {}", ticker.getTickerSymbol());
            }
        });

        log.info("Completed Renko Data Computation");
    }

    private List<RenkoData> generateRenkoBricks(Ticker ticker, List<MarketData> allSeries) {
        BigDecimal brickSize = calculateBrickSize(allSeries);
        if (brickSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Calculated brick size is zero or negative for {}. Using default 0.1", ticker.getTickerSymbol());
            brickSize = new BigDecimal("0.1");
        }

        List<RenkoData> renkoBricks = new ArrayList<>();
        BigDecimal currentPrice = allSeries.get(0).getVwap();

        for (MarketData row : allSeries) {
            BigDecimal vwap = row.getVwap();
            LocalDate date = row.getMarketDataDate();

            // Up bricks
            while (vwap.compareTo(currentPrice.add(brickSize)) >= 0) {
                currentPrice = currentPrice.add(brickSize);
                RenkoData brick = new RenkoData();
                brick.setTicker(ticker);
                brick.setRenkoDate(date);
                brick.setBrickLow(currentPrice.subtract(brickSize));
                brick.setBrickHigh(currentPrice);
                brick.setDirection("up");
                renkoBricks.add(brick);
            }

            // Down bricks
            while (vwap.compareTo(currentPrice.subtract(brickSize)) <= 0) {
                currentPrice = currentPrice.subtract(brickSize);
                RenkoData brick = new RenkoData();
                brick.setTicker(ticker);
                brick.setRenkoDate(date);
                brick.setBrickLow(currentPrice);
                brick.setBrickHigh(currentPrice.add(brickSize));
                brick.setDirection("down");
                renkoBricks.add(brick);
            }
        }

        if (renkoBricks.isEmpty()) return renkoBricks;

        List<RenkoData> filteredBricks = removeConsecutiveDuplicates(renkoBricks);
        calculateZoneTrend(filteredBricks);

        return filteredBricks;
    }

    private BigDecimal calculateBrickSize(List<MarketData> allSeries) {
        int lookbackStart = Math.max(0, allSeries.size() - PERIOD_COUNT);
        List<MarketData> lookbackData = allSeries.subList(lookbackStart, allSeries.size());

        BigDecimal totalRange = BigDecimal.ZERO;
        for (MarketData row : lookbackData) {
            BigDecimal range = row.getPriceHigh().subtract(row.getPriceLow());
            totalRange = totalRange.add(range);
        }

        BigDecimal avgRange = totalRange.divide(valueOf(lookbackData.size()), DB_MATH_CONTEXT);
        return avgRange.divide(valueOf(2), DB_MATH_CONTEXT);
    }

    private List<RenkoData> removeConsecutiveDuplicates(List<RenkoData> bricks) {
        if (bricks.size() <= 1) return bricks;
        List<RenkoData> filtered = new ArrayList<>();
        filtered.add(bricks.get(0));

        for (int i = 1; i < bricks.size(); i++) {
            RenkoData current = bricks.get(i);
            RenkoData previous = filtered.get(filtered.size() - 1);

            if (current.getBrickLow().compareTo(previous.getBrickLow()) != 0 ||
                current.getBrickHigh().compareTo(previous.getBrickHigh()) != 0) {
                filtered.add(current);
            }
        }
        return filtered;
    }

    private void calculateZoneTrend(List<RenkoData> bricks) {
        String currentDir = "up"; // The first renko brick is always up.
        int previousUptrend = 0;
        int previousDowntrend = 0;
        int trend = 0;

        for (RenkoData brick : bricks) {
            if (brick.getDirection().equals(currentDir)) {
                trend++;
            } else {
                // Direction changed
                if (currentDir.equals("up")) {
                    previousUptrend = trend;
                } else {
                    previousDowntrend = trend;
                }
                currentDir = brick.getDirection();

                // Resumption logic
                if (currentDir.equals("up")) {
                    if (trend <= getZoneFromTrend(previousUptrend)) {
                        trend = (previousUptrend - trend) + 1;
                    } else {
                        trend = 1;
                    }
                } else {
                    if (trend <= getZoneFromTrend(previousDowntrend)) {
                        trend = (previousDowntrend - trend) + 1;
                    } else {
                        trend = 1;
                    }
                }
            }
            brick.setTrend(trend);
        }
    }

    private int getZoneFromTrend(int trend) {
        if (trend <= 3) return 0;
        if (trend <= 9) return 1;
        if (trend <= 27) return 2;
        if (trend <= 81) return 3;
        return 4;
    }
}
