package com.alphaflow.infrastructure.generators;

import com.alphaflow.infrastructure.entities.CandleBar;
import com.alphaflow.infrastructure.entities.RenkoBrick;
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

public class RenkoBricksGenerator {

    private static final Logger log = LoggerFactory.getLogger(RenkoBricksGenerator.class);

    public static List<RenkoBrick> generateRenkoBricks(Ticker ticker, List<CandleBar> allSeries) {
        return generateRenkoBricks(ticker, allSeries, RenkoPriceSource.PRICE_CLOSE);
    }

    public static List<RenkoBrick> generateRenkoBricks(Ticker ticker, List<CandleBar> allSeries, RenkoPriceSource priceSource) {
        log.debug("Generating Renko bricks for {} using {}", ticker.getTickerSymbol(), priceSource);
        BigDecimal brickSize = calculateBrickSize(allSeries);
        if (brickSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Calculated brick size is zero or negative for {}. Skipping generation.", ticker.getTickerSymbol());
            return List.of();
        }

        List<RenkoBrick> renkoBricks = new ArrayList<>();
        BigDecimal currentPrice = priceSource.getPrice(allSeries.getFirst());

        for (CandleBar row : allSeries) {
            BigDecimal price = priceSource.getPrice(row);
            LocalDate date = row.getCandleBarDate();

            // Up bricks
            while (price.compareTo(currentPrice.add(brickSize)) >= 0) {
                currentPrice = currentPrice.add(brickSize);
                renkoBricks.add(RenkoBrick.builder()
                        .ticker(ticker)
                        .renkoBrickDate(date)
                        .brickLow(currentPrice.subtract(brickSize))
                        .brickHigh(currentPrice)
                        .direction(RENKO_BRICK_DIRECTION_UP)
                        .build());
            }

            // Down bricks
            while (price.compareTo(currentPrice.subtract(brickSize)) <= 0) {
                currentPrice = currentPrice.subtract(brickSize);
                renkoBricks.add(RenkoBrick.builder()
                        .ticker(ticker)
                        .renkoBrickDate(date)
                        .brickLow(currentPrice)
                        .brickHigh(currentPrice.add(brickSize))
                        .direction(RENKO_BRICK_DIRECTION_DOWN)
                        .build());
            }
        }

        if (renkoBricks.isEmpty()) return renkoBricks;

        return calculateZoneTrend(removeConsecutiveDuplicates(renkoBricks));
    }

    private static BigDecimal calculateBrickSize(List<CandleBar> allSeries) {
        int loopbackStart = Math.max(0, allSeries.size() - RENKO_BRICK_SIZE_PERIOD);
        List<CandleBar> loopbackCandleBar = allSeries.subList(loopbackStart, allSeries.size());

        BigDecimal totalRange = BigDecimal.ZERO;
        for (CandleBar candleBar : loopbackCandleBar) {
            totalRange = totalRange.add(
                    candleBar.getPriceHigh().subtract(candleBar.getPriceLow())
            );
        }

        BigDecimal avgRange = totalRange.divide(valueOf(RENKO_BRICK_SIZE_PERIOD), DB_MATH_CONTEXT);
        return avgRange.divide(valueOf(2), DB_MATH_CONTEXT);
    }

    private static List<RenkoBrick> removeConsecutiveDuplicates(List<RenkoBrick> bricks) {
        if (bricks.size() <= 1) return bricks;
        List<RenkoBrick> filtered = new ArrayList<>();
        filtered.add(bricks.getFirst());

        for (int i = 1; i < bricks.size(); i++) {
            RenkoBrick current = bricks.get(i);
            RenkoBrick previous = filtered.getLast();

            if (current.getBrickLow().compareTo(previous.getBrickLow()) != 0 ||
                    current.getBrickHigh().compareTo(previous.getBrickHigh()) != 0) {
                filtered.add(current);
            }
        }
        return filtered;
    }

    private static List<RenkoBrick> calculateZoneTrend(List<RenkoBrick> bricks) {
        String currentDir = RENKO_BRICK_DIRECTION_UP; // The first renko brick is always up.
        int previousUptrend = 0;
        int previousDowntrend = 0;
        int trend = 0;

        for (RenkoBrick brick : bricks) {
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
