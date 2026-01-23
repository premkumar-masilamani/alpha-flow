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

import static com.alphaflow.infrastructure.config.Constants.*;
import static com.alphaflow.infrastructure.util.RenkoUtil.getZoneFromTrend;
import static java.math.BigDecimal.valueOf;

@Service
public class RenkoDataComputer {

    private static final Logger log = LoggerFactory.getLogger(RenkoDataComputer.class);

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
                    ticker, EPOCH_START);

            if (allSeries.isEmpty()) {
                log.error("No market data found for {}", ticker.getTickerSymbol());
                return;
            }

            // TODO: Add logic to detect existing Renko data and NOT reprocess it

            renkoDataRepository.deleteByTicker(ticker);
            renkoDataRepository.flush();

            List<RenkoData> renkoBricks = generateRenkoBricks(ticker, allSeries);
            if (renkoBricks.isEmpty()) {
                log.error("No Renko bricks generated for {}", ticker.getTickerSymbol());
                return;
            }

            renkoDataRepository.saveAll(renkoBricks);
            log.info("Generated {} Renko bricks for {}", renkoBricks.size(), ticker.getTickerSymbol());
        });

        log.info("Completed Renko Data Computation");
    }

    private List<RenkoData> generateRenkoBricks(Ticker ticker, List<MarketData> allSeries) {
        BigDecimal brickSize = calculateBrickSize(allSeries);
        if (brickSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Calculated brick size is zero or negative ({}) for {}. Failing computation.", brickSize, ticker.getTickerSymbol());
            return List.of();
        }

        List<RenkoData> renkoBricks = new ArrayList<>();
        BigDecimal currentPrice = allSeries.getFirst().getVwap();

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
                brick.setDirection(RENKO_BRICK_DIRECTION_UP);
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
                brick.setDirection(RENKO_BRICK_DIRECTION_DOWN);
                renkoBricks.add(brick);
            }
        }

        if (renkoBricks.isEmpty()) return renkoBricks;

        return calculateZoneTrend(removeConsecutiveDuplicates(renkoBricks));
    }

    private BigDecimal calculateBrickSize(List<MarketData> allSeries) {
        int loopbackStart = Math.max(0, allSeries.size() - RENKO_BRICK_SIZE_PERIOD);
        List<MarketData> loopbackMarketData = allSeries.subList(loopbackStart, allSeries.size());

        BigDecimal totalRange = BigDecimal.ZERO;
        for (MarketData marketData : loopbackMarketData) {
            totalRange = totalRange.add(
                    marketData.getPriceHigh().subtract(marketData.getPriceLow())
            );
        }

        BigDecimal avgRange = totalRange.divide(valueOf(RENKO_BRICK_SIZE_PERIOD), DB_MATH_CONTEXT);
        BigDecimal brickSize = avgRange.divide(valueOf(2), DB_MATH_CONTEXT);

        log.debug("Renko Brick size: {}", brickSize);
        return brickSize;
    }

    private List<RenkoData> removeConsecutiveDuplicates(List<RenkoData> bricks) {
        if (bricks.size() <= 1) return bricks;
        List<RenkoData> filtered = new ArrayList<>();
        filtered.add(bricks.getFirst());

        for (int i = 1; i < bricks.size(); i++) {
            RenkoData current = bricks.get(i);
            RenkoData previous = filtered.getLast();

            if (current.getBrickLow().compareTo(previous.getBrickLow()) != 0 ||
                    current.getBrickHigh().compareTo(previous.getBrickHigh()) != 0) {
                filtered.add(current);
            }
        }
        return filtered;
    }

    private List<RenkoData> calculateZoneTrend(List<RenkoData> bricks) {
        String currentDir = RENKO_BRICK_DIRECTION_UP; // The first renko brick is always up.
        int previousUptrend = 0;
        int previousDowntrend = 0;
        int trend = 0;

        for (RenkoData brick : bricks) {
            if (brick.getDirection().equals(currentDir)) {
                trend++;
            } else {
                // Direction changed
                if (currentDir.equals(RENKO_BRICK_DIRECTION_UP)) {
                    previousUptrend = trend;
                } else {
                    previousDowntrend = trend;
                }
                currentDir = brick.getDirection();

                // Resumption logic
                if (currentDir.equals(RENKO_BRICK_DIRECTION_UP)) {
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
            brick.setZone(getZoneFromTrend(trend));
        }
        return bricks;
    }
}
