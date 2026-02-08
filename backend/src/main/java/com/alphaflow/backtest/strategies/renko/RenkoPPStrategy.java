package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestStrategyIndicator;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphaflow.infrastructure.constants.AppConstants.RENKO_BRICK_DIRECTION_DOWN;
import static com.alphaflow.infrastructure.constants.AppConstants.RENKO_BRICK_DIRECTION_UP;

@Component
public class RenkoPPStrategy implements RenkoStrategy {

    private static final Logger log = LoggerFactory.getLogger(RenkoPPStrategy.class);

    private static final String PREV_SHORT_SPREAD_LONG = "PREV_SHORT_SPREAD_LONG";
    private static final String PREV_LONG_SPREAD_LONG = "PREV_LONG_SPREAD_LONG";
    private static final String PREV_SHORT_SPREAD_SHORT = "PREV_SHORT_SPREAD_SHORT";
    private static final String PREV_LONG_SPREAD_SHORT = "PREV_LONG_SPREAD_SHORT";

    @Override
    public String getName() {
        return "Renko PP";
    }

    @Override
    public RenkoPriceSource getPriceSource() {
        return RenkoPriceSource.PRICE_CLOSE;
    }

    @Override
    public BacktestStrategy getEntity() {
        BacktestStrategy strategy = BacktestStrategy.builder()
                .name(getName())
                .strategyType("RENKO_PP")
                .priceSource(RenkoPriceSource.PRICE_CLOSE)
                .build();

        List<BacktestStrategyIndicator> indicators = new ArrayList<>();
        int[] periods = {3, 5, 8, 10, 12, 15, 30, 35, 40, 45, 50, 60};
        for (int p : periods) {
            indicators.add(BacktestStrategyIndicator.builder()
                    .backtestStrategy(strategy)
                    .indicatorRole("GMMA")
                    .metric("P_CLOSE")
                    .transformation("EMA")
                    .period(p)
                    .build());
        }
        strategy.setIndicators(indicators);
        return strategy;
    }

    @Override
    public TradeAction generateSignal(StrategyContext context) {
        List<RenkoData> renkoBricks = context.renkoBricks();
        Map<String, BigDecimal> indicators = context.indicators();
        PositionType currentPosition = context.currentPosition();
        Map<String, Object> state = context.state();

        if (renkoBricks == null || renkoBricks.isEmpty() || context.marketData() == null) {
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        RenkoData currentBrick = renkoBricks.getLast();
        int trend = currentBrick.getTrend();
        int zone = currentBrick.getZone();
        String direction = currentBrick.getDirection();
        BigDecimal priceClose = context.marketData().getPriceClose();

        // GMMA EMAs
        BigDecimal ema3 = indicators.get("P_CLOSE_EMA_3");
        BigDecimal ema5 = indicators.get("P_CLOSE_EMA_5");
        BigDecimal ema8 = indicators.get("P_CLOSE_EMA_8");
        BigDecimal ema10 = indicators.get("P_CLOSE_EMA_10");
        BigDecimal ema12 = indicators.get("P_CLOSE_EMA_12");
        BigDecimal ema15 = indicators.get("P_CLOSE_EMA_15");
        BigDecimal ema30 = indicators.get("P_CLOSE_EMA_30");
        BigDecimal ema35 = indicators.get("P_CLOSE_EMA_35");
        BigDecimal ema40 = indicators.get("P_CLOSE_EMA_40");
        BigDecimal ema45 = indicators.get("P_CLOSE_EMA_45");
        BigDecimal ema50 = indicators.get("P_CLOSE_EMA_50");
        BigDecimal ema60 = indicators.get("P_CLOSE_EMA_60");

        if (ema3 == null || ema5 == null || ema8 == null || ema10 == null || ema12 == null || ema15 == null ||
                ema30 == null || ema35 == null || ema40 == null || ema45 == null || ema50 == null || ema60 == null) {
            log.debug("Strategy {} missing GMMA indicators", getName());
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        // Current Spreads
        BigDecimal shortSpreadLong = ema3.subtract(ema15);
        BigDecimal longSpreadLong = ema30.subtract(ema60);
        BigDecimal shortSpreadShort = ema15.subtract(ema3);
        BigDecimal longSpreadShort = ema60.subtract(ema30);

        // Previous Spreads from state
        BigDecimal prevShortSpreadLong = (BigDecimal) state.get(PREV_SHORT_SPREAD_LONG);
        BigDecimal prevLongSpreadLong = (BigDecimal) state.get(PREV_LONG_SPREAD_LONG);
        BigDecimal prevShortSpreadShort = (BigDecimal) state.get(PREV_SHORT_SPREAD_SHORT);
        BigDecimal prevLongSpreadShort = (BigDecimal) state.get(PREV_LONG_SPREAD_SHORT);

        // Update state for next call
        state.put(PREV_SHORT_SPREAD_LONG, shortSpreadLong);
        state.put(PREV_LONG_SPREAD_LONG, longSpreadLong);
        state.put(PREV_SHORT_SPREAD_SHORT, shortSpreadShort);
        state.put(PREV_LONG_SPREAD_SHORT, longSpreadShort);

        // ───── EXIT Logic (Trailing Stop Loss) ─────
        if (currentPosition == PositionType.LONG) {
            int slIndex = (zone == 0) ? 1 : (trend - zone - 1);
            if (slIndex >= 1 && slIndex <= trend) {
                int listIdx = renkoBricks.size() - (trend - slIndex + 1);
                if (listIdx >= 0) {
                    BigDecimal slPrice = renkoBricks.get(listIdx).getBrickLow();
                    if (priceClose.compareTo(slPrice) < 0) {
                        log.debug("Strategy {} triggering LONG SL EXIT at {} price {} SL {}", getName(), context.marketData().getMarketDataDate(), priceClose, slPrice);
                        return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
                    }
                }
            }
        } else if (currentPosition == PositionType.SHORT) {
            int slIndex = (zone == 0) ? 1 : (trend - zone - 1);
            if (slIndex >= 1 && slIndex <= trend) {
                int listIdx = renkoBricks.size() - (trend - slIndex + 1);
                if (listIdx >= 0) {
                    BigDecimal slPrice = renkoBricks.get(listIdx).getBrickHigh();
                    if (priceClose.compareTo(slPrice) > 0) {
                        log.debug("Strategy {} triggering SHORT SL EXIT at {} price {} SL {}", getName(), context.marketData().getMarketDataDate(), priceClose, slPrice);
                        return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
                    }
                }
            }
        }

        // ───── ENTRY Logic ─────
        if (prevShortSpreadLong == null || prevLongSpreadLong == null ||
                prevShortSpreadShort == null || prevLongSpreadShort == null) {
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        Map<String, Object> signalData = new HashMap<>();

        // Long Entry
        if (direction.equals(RENKO_BRICK_DIRECTION_UP)) {
            // 1. Bricks / Zone Criteria
            boolean brickCriteria = (trend >= 4 && trend <= 9); // Zone 1
            if (brickCriteria) {
                // Minimum 3 Red Bricks before the 1st Green Brick
                int firstGreenIdx = renkoBricks.size() - trend;
                int lastRedIdx = firstGreenIdx - 1;
                if (lastRedIdx >= 0) {
                    RenkoData lastRedBrick = renkoBricks.get(lastRedIdx);
                    brickCriteria = lastRedBrick.getDirection().equals(RENKO_BRICK_DIRECTION_DOWN) && lastRedBrick.getTrend() >= 3;
                } else {
                    brickCriteria = false;
                }
            }

            // 2. GMMA Criteria
            boolean gmmaOrder = (ema3.compareTo(ema5) > 0 && ema5.compareTo(ema8) > 0 && ema8.compareTo(ema10) > 0 &&
                    ema10.compareTo(ema12) > 0 && ema12.compareTo(ema15) > 0 &&
                    ema15.compareTo(ema30) > 0 &&
                    ema30.compareTo(ema35) > 0 && ema35.compareTo(ema40) > 0 && ema40.compareTo(ema45) > 0 &&
                    ema45.compareTo(ema50) > 0 && ema50.compareTo(ema60) > 0);

            boolean gmmaExpansion = shortSpreadLong.compareTo(prevShortSpreadLong) > 0 &&
                    longSpreadLong.compareTo(prevLongSpreadLong) > 0;

            if (brickCriteria && gmmaOrder && gmmaExpansion && currentPosition != PositionType.LONG) {
                log.debug("Strategy {} generating ENTER_LONG signal at {}", getName(), context.marketData().getMarketDataDate());
                signalData.put("trend", trend);
                signalData.put("zone", zone);
                signalData.put("shortSpread", shortSpreadLong);
                signalData.put("longSpread", longSpreadLong);
                return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG, signalData);
            }
        }

        // Short Entry
        if (direction.equals(RENKO_BRICK_DIRECTION_DOWN)) {
            // 1. Bricks / Zone Criteria
            boolean brickCriteria = (trend >= 4 && trend <= 9); // Zone 1
            if (brickCriteria) {
                // Minimum 3 Green Bricks before the 1st Red Brick
                int firstRedIdx = renkoBricks.size() - trend;
                int lastGreenIdx = firstRedIdx - 1;
                if (lastGreenIdx >= 0) {
                    RenkoData lastGreenBrick = renkoBricks.get(lastGreenIdx);
                    brickCriteria = lastGreenBrick.getDirection().equals(RENKO_BRICK_DIRECTION_UP) && lastGreenBrick.getTrend() >= 3;
                } else {
                    brickCriteria = false;
                }
            }

            // 2. GMMA Criteria
            boolean gmmaOrder = (ema3.compareTo(ema5) < 0 && ema5.compareTo(ema8) < 0 && ema8.compareTo(ema10) < 0 &&
                    ema10.compareTo(ema12) < 0 && ema12.compareTo(ema15) < 0 &&
                    ema15.compareTo(ema30) < 0 &&
                    ema30.compareTo(ema35) < 0 && ema35.compareTo(ema40) < 0 && ema40.compareTo(ema45) < 0 &&
                    ema45.compareTo(ema50) < 0 && ema50.compareTo(ema60) < 0);

            boolean gmmaExpansion = shortSpreadShort.compareTo(prevShortSpreadShort) > 0 &&
                    longSpreadShort.compareTo(prevLongSpreadShort) > 0;

            if (brickCriteria && gmmaOrder && gmmaExpansion && currentPosition != PositionType.SHORT) {
                log.debug("Strategy {} generating ENTER_SHORT signal at {}", getName(), context.marketData().getMarketDataDate());
                signalData.put("trend", trend);
                signalData.put("zone", zone);
                signalData.put("shortSpread", shortSpreadShort);
                signalData.put("longSpread", longSpreadShort);
                return new TradeAction(TradeSignal.ENTER_SHORT, PositionType.SHORT, signalData);
            }
        }

        return new TradeAction(TradeSignal.HOLD, currentPosition);
    }
}
