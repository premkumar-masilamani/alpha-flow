package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import com.alphaflow.domain.enums.TradeSignal;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.alphaflow.infrastructure.config.Constants.RENKO_BRICK_DIRECTION_DOWN;
import static com.alphaflow.infrastructure.config.Constants.RENKO_BRICK_DIRECTION_UP;

/**
 * Renko TSM Strategy implementation.
 */
@Component
public class RenkoTSMStrategy implements RenkoBacktestStrategy {

    private static final String STATE_STOP_LOSS = "stopLoss";
    private static final String STATE_LAST_OBV = "lastObv";

    // Indicator key format: metricCode_maType_period
    private static final String VWAP_SMA_10 = "VWAP_SMA_10";
    private static final String OBV_KEY = "OBV_OBV_0";

    @Override
    public String getName() {
        return "Renko TSM";
    }

    @Override
    public TradeSignal generateSignal(
            MarketData currentDay,
            List<RenkoData> renkoBricks,
            Map<String, BigDecimal> indicators,
            PositionType currentPosition,
            Map<String, Object> strategyState
    ) {
        BigDecimal vwapSma10 = indicators.get(VWAP_SMA_10);
        BigDecimal currentObv = indicators.get(OBV_KEY);
        BigDecimal lastObv = (BigDecimal) strategyState.get(STATE_LAST_OBV);
        BigDecimal stopLoss = (BigDecimal) strategyState.get(STATE_STOP_LOSS);

        // Update state with current OBV for next day's comparison
        strategyState.put(STATE_LAST_OBV, currentObv);

        if (vwapSma10 == null || currentObv == null) {
            return new TradeSignal(TradeAction.NO_SIGNAL, currentPosition);
        }

        LocalDate today = currentDay.getMarketDataDate();
        List<RenkoData> newBricksToday = renkoBricks.stream()
                .filter(b -> b.getRenkoDate().isEqual(today))
                .toList();

        // ─────────────────────────────
        // 1. Check for Exit Conditions (including Stop Loss)
        // ─────────────────────────────
        if (currentPosition != PositionType.NONE) {
            if (isLong(currentPosition)) {
                // SL Hit? (using current day low)
                if (stopLoss != null && currentDay.getPriceLow().compareTo(stopLoss) < 0) {
                    strategyState.remove(STATE_STOP_LOSS);
                    return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
                }
                // Price close below SMA10?
                if (currentDay.getPriceClose().compareTo(vwapSma10) < 0) {
                    strategyState.remove(STATE_STOP_LOSS);
                    return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
                }
            } else if (isShort(currentPosition)) {
                // SL Hit? (using current day high)
                if (stopLoss != null && currentDay.getPriceHigh().compareTo(stopLoss) > 0) {
                    strategyState.remove(STATE_STOP_LOSS);
                    return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
                }
                // Price close above SMA10?
                if (currentDay.getPriceClose().compareTo(vwapSma10) > 0) {
                    strategyState.remove(STATE_STOP_LOSS);
                    return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
                }
            }
        }

        // ─────────────────────────────
        // 2. Trailing Stop Loss Update (for existing positions)
        // ─────────────────────────────
        if (currentPosition != PositionType.NONE) {
            for (RenkoData brick : newBricksToday) {
                if (isLong(currentPosition) && brick.getDirection().equals(RENKO_BRICK_DIRECTION_UP)) {
                    BigDecimal brickSize = brick.getBrickHigh().subtract(brick.getBrickLow());
                    BigDecimal newSL = brick.getBrickHigh().subtract(brickSize.multiply(BigDecimal.valueOf(3)));
                    if (stopLoss == null || newSL.compareTo(stopLoss) > 0) {
                        stopLoss = newSL;
                        strategyState.put(STATE_STOP_LOSS, stopLoss);
                    }
                } else if (isShort(currentPosition) && brick.getDirection().equals(RENKO_BRICK_DIRECTION_DOWN)) {
                    BigDecimal brickSize = brick.getBrickHigh().subtract(brick.getBrickLow());
                    BigDecimal newSL = brick.getBrickLow().add(brickSize.multiply(BigDecimal.valueOf(3)));
                    if (stopLoss == null || newSL.compareTo(stopLoss) < 0) {
                        stopLoss = newSL;
                        strategyState.put(STATE_STOP_LOSS, stopLoss);
                    }
                }
            }
        }

        // ─────────────────────────────
        // 3. Entry Signals
        // ─────────────────────────────
        boolean obvIncreasing = lastObv != null && currentObv.compareTo(lastObv) > 0;
        boolean obvDecreasing = lastObv != null && currentObv.compareTo(lastObv) < 0;

        // Long Entry conditions
        boolean hasNewUpBrickAboveSMA = newBricksToday.stream()
                .anyMatch(b -> b.getDirection().equals(RENKO_BRICK_DIRECTION_UP) && b.getBrickHigh().compareTo(vwapSma10) > 0);

        if (hasNewUpBrickAboveSMA && obvIncreasing && !isLong(currentPosition)) {
            // Set initial SL based on the latest UP brick formed today
            RenkoData latestUpBrick = newBricksToday.stream()
                    .filter(b -> b.getDirection().equals(RENKO_BRICK_DIRECTION_UP))
                    .reduce((first, second) -> second).orElseThrow();
            BigDecimal brickSize = latestUpBrick.getBrickHigh().subtract(latestUpBrick.getBrickLow());
            BigDecimal initialSL = latestUpBrick.getBrickHigh().subtract(brickSize.multiply(BigDecimal.valueOf(3)));
            strategyState.put(STATE_STOP_LOSS, initialSL);

            return new TradeSignal(TradeAction.ENTER_LONG, PositionType.LONG_100);
        }

        // Short Entry conditions
        boolean hasNewDownBrickBelowSMA = newBricksToday.stream()
                .anyMatch(b -> b.getDirection().equals(RENKO_BRICK_DIRECTION_DOWN) && b.getBrickLow().compareTo(vwapSma10) < 0);

        if (hasNewDownBrickBelowSMA && obvDecreasing && !isShort(currentPosition)) {
            // Set initial SL based on the latest DOWN brick formed today
            RenkoData latestDownBrick = newBricksToday.stream()
                    .filter(b -> b.getDirection().equals(RENKO_BRICK_DIRECTION_DOWN))
                    .reduce((first, second) -> second).orElseThrow();
            BigDecimal brickSize = latestDownBrick.getBrickHigh().subtract(latestDownBrick.getBrickLow());
            BigDecimal initialSL = latestDownBrick.getBrickLow().add(brickSize.multiply(BigDecimal.valueOf(3)));
            strategyState.put(STATE_STOP_LOSS, initialSL);

            return new TradeSignal(TradeAction.ENTER_SHORT, PositionType.SHORT_100);
        }

        return new TradeSignal(TradeAction.HOLD, currentPosition);
    }

    private boolean isLong(PositionType position) {
        return position == PositionType.LONG_25 || position == PositionType.LONG_50 || position == PositionType.LONG_100;
    }

    private boolean isShort(PositionType position) {
        return position == PositionType.SHORT_25 || position == PositionType.SHORT_50 || position == PositionType.SHORT_100;
    }
}
