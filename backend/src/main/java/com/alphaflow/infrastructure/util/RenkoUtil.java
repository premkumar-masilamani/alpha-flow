package com.alphaflow.infrastructure.util;

import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.alphaflow.infrastructure.config.Constants.*;
import static java.math.BigDecimal.valueOf;

public class RenkoUtil {

    private static final Logger log = LoggerFactory.getLogger(RenkoUtil.class);

    public static int getZoneFromTrend(int trend) {
        if (trend <= 3) return 0;
        if (trend <= 9) return 1;
        if (trend <= 27) return 2;
        if (trend <= 81) return 3;
        return 4;
    }

    public static BigDecimal calculateBrickSize(List<MarketData> allSeries) {
        int loopbackStart = Math.max(0, allSeries.size() - RENKO_BRICK_SIZE_PERIOD);
        List<MarketData> loopbackMarketData = allSeries.subList(loopbackStart, allSeries.size());

        log.info("Using the following periods for brick size calculation:");
        BigDecimal totalRange = BigDecimal.ZERO;
        for (MarketData marketData : loopbackMarketData) {
            BigDecimal range = marketData.getPriceHigh().subtract(marketData.getPriceLow());
            totalRange = totalRange.add(range);
            log.info("  {}: High = {}, Low = {}, Range = {}",
                marketData.getMarketDataDate(), marketData.getPriceHigh(), marketData.getPriceLow(), range);
        }

        BigDecimal avgRange = totalRange.divide(valueOf(RENKO_BRICK_SIZE_PERIOD), DB_MATH_CONTEXT);
        BigDecimal brickSize = avgRange.divide(valueOf(2), DB_MATH_CONTEXT);

        log.info("Total range: {}", totalRange);
        log.info("Average range: {}", avgRange);
        log.info("Brick size (half of average range): {}", brickSize);

        return brickSize;
    }

    public static List<RenkoData> generateRenkoBricks(Ticker ticker, List<MarketData> allSeries, BigDecimal brickSize) {
        if (brickSize.compareTo(BigDecimal.ZERO) <= 0) {
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

    public static List<RenkoData> removeConsecutiveDuplicates(List<RenkoData> bricks) {
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

    public static List<RenkoData> calculateZoneTrend(List<RenkoData> bricks) {
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
