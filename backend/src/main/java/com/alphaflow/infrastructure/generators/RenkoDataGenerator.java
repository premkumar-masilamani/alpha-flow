package com.alphaflow.infrastructure.generators;

import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.alphaflow.infrastructure.constants.AppConstants.*;
import static java.math.BigDecimal.valueOf;

public class RenkoDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(RenkoDataGenerator.class);

    public static List<RenkoData> generateRenkoData(Ticker ticker, List<CandleData> allSeries) {
        return generateRenkoData(ticker, allSeries, RenkoPriceSource.PRICE_CLOSE);
    }

    public static List<RenkoData> generateRenkoData(Ticker ticker, List<CandleData> allSeries, RenkoPriceSource priceSource) {
        log.debug("Generating Renko data for {} using {}", ticker.getTickerSymbol(), priceSource);
        BigDecimal brickSize = calculateBrickSize(allSeries);
        if (brickSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Calculated brick size is zero or negative for {}. Skipping generation.", ticker.getTickerSymbol());
            return List.of();
        }

        List<RenkoData> renkoData = new ArrayList<>();
        BigDecimal currentPrice = priceSource.getPrice(allSeries.getFirst());

        for (CandleData row : allSeries) {
            BigDecimal price = priceSource.getPrice(row);
            LocalDate date = row.getCandleDataDate();

            // Up bricks
            while (price.compareTo(currentPrice.add(brickSize)) >= 0) {
                currentPrice = currentPrice.add(brickSize);
                renkoData.add(RenkoData.builder()
                        .ticker(ticker)
                        .renkoDataDate(date)
                        .brickLow(currentPrice.subtract(brickSize))
                        .brickHigh(currentPrice)
                        .direction(RENKO_BRICK_DIRECTION_UP)
                        .build());
            }

            // Down bricks
            while (price.compareTo(currentPrice.subtract(brickSize)) <= 0) {
                currentPrice = currentPrice.subtract(brickSize);
                renkoData.add(RenkoData.builder()
                        .ticker(ticker)
                        .renkoDataDate(date)
                        .brickLow(currentPrice)
                        .brickHigh(currentPrice.add(brickSize))
                        .direction(RENKO_BRICK_DIRECTION_DOWN)
                        .build());
            }
        }

        if (renkoData.isEmpty()) return renkoData;

        return calculateZoneTrend(removeConsecutiveDuplicates(renkoData));
    }

    private static BigDecimal calculateBrickSize(List<CandleData> allSeries) {
        int loopbackStart = Math.max(0, allSeries.size() - RENKO_BRICK_SIZE_PERIOD);
        List<CandleData> loopbackCandleData = allSeries.subList(loopbackStart, allSeries.size());

        BigDecimal totalRange = BigDecimal.ZERO;
        for (CandleData candleData : loopbackCandleData) {
            totalRange = totalRange.add(
                    candleData.getPriceHigh().subtract(candleData.getPriceLow())
            );
        }

        BigDecimal avgRange = totalRange.divide(valueOf(RENKO_BRICK_SIZE_PERIOD), DB_MATH_CONTEXT);
        return avgRange.divide(valueOf(2), DB_MATH_CONTEXT);
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
        String currentDir = RENKO_BRICK_DIRECTION_UP; // The first renko data point is always up.
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

    public static int getZoneFromTrend(int trend) {
        if (trend <= 3) return 0;
        if (trend <= 9) return 1;
        if (trend <= 27) return 2;
        if (trend <= 81) return 3;
        return 4;
    }

}
