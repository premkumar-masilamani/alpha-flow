package com.alphaflow.infrastructure.util;

import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.alphaflow.infrastructure.config.Constants.*;
import static java.math.BigDecimal.valueOf;

public class RenkoUtil {

    public static int getZoneFromTrend(int trend) {
        if (trend <= 3) return 0;
        if (trend <= 9) return 1;
        if (trend <= 27) return 2;
        if (trend <= 81) return 3;
        return 4;
    }

    public static List<RenkoData> generateRenkoBricks(Ticker ticker, List<MarketData> allSeries, Function<MarketData, BigDecimal> priceExtractor) {
        BigDecimal brickSize = calculateBrickSize(allSeries);
        if (brickSize.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<RenkoData> renkoBricks = new ArrayList<>();
        BigDecimal currentPrice = priceExtractor.apply(allSeries.getFirst());

        for (MarketData row : allSeries) {
            BigDecimal price = priceExtractor.apply(row);
            LocalDate date = row.getMarketDataDate();

            // Up bricks
            while (price.compareTo(currentPrice.add(brickSize)) >= 0) {
                currentPrice = currentPrice.add(brickSize);
                renkoBricks.add(RenkoData.builder()
                        .ticker(ticker)
                        .renkoDate(date)
                        .brickLow(currentPrice.subtract(brickSize))
                        .brickHigh(currentPrice)
                        .direction(RENKO_BRICK_DIRECTION_UP)
                        .build());
            }

            // Down bricks
            while (price.compareTo(currentPrice.subtract(brickSize)) <= 0) {
                currentPrice = currentPrice.subtract(brickSize);
                renkoBricks.add(RenkoData.builder()
                        .ticker(ticker)
                        .renkoDate(date)
                        .brickLow(currentPrice)
                        .brickHigh(currentPrice.add(brickSize))
                        .direction(RENKO_BRICK_DIRECTION_DOWN)
                        .build());
            }
        }

        if (renkoBricks.isEmpty()) return renkoBricks;

        return calculateZoneTrend(removeConsecutiveDuplicates(renkoBricks));
    }

    private static BigDecimal calculateBrickSize(List<MarketData> allSeries) {
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

        return brickSize;
    }

    private static List<RenkoData> removeConsecutiveDuplicates(List<RenkoData> bricks) {
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

    private static List<RenkoData> calculateZoneTrend(List<RenkoData> bricks) {
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
