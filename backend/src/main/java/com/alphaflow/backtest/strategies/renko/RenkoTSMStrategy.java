package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.RenkoData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.alphaflow.backtest.enums.PositionType.LONG;
import static com.alphaflow.backtest.enums.PositionType.SHORT;
import static com.alphaflow.infrastructure.constants.AppConstants.RENKO_BRICK_DIRECTION_DOWN;
import static com.alphaflow.infrastructure.constants.AppConstants.RENKO_BRICK_DIRECTION_UP;

@Component
public class RenkoTSMStrategy implements RenkoBacktestStrategy {

    @Override
    public String getName() {
        return "Renko TSM";
    }

    @Override
    public TradeAction generateSignal(
            List<RenkoData> renkoBricks,
            MarketData currentDayMarketData,
            Map<String, BigDecimal> currentDayIndicators,
            PositionType currentDayPosition,
            Map<String, Object> strategyState
    ) {
        if (renkoBricks.isEmpty()) {
            return new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);
        }

        RenkoData lastBrick = renkoBricks.getLast();
        BigDecimal sma10 = currentDayIndicators.get("P_CLOSE_SMA_10");
        BigDecimal obv = currentDayIndicators.get("OBV_OBV_0");
        BigDecimal prevObv = (BigDecimal) strategyState.getOrDefault("prevObv", BigDecimal.ZERO);
        strategyState.put("prevObv", obv);

        if (sma10 == null || obv == null) {
            return new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);
        }

        boolean isNewBrickToday = lastBrick.getRenkoDate().isEqual(currentDayMarketData.getMarketDataDate());
        boolean isUpBrick = lastBrick.getDirection().equals(RENKO_BRICK_DIRECTION_UP);
        boolean isDownBrick = lastBrick.getDirection().equals(RENKO_BRICK_DIRECTION_DOWN);

        // Entry conditions
        boolean longEntry = isNewBrickToday && isUpBrick && lastBrick.getBrickHigh().compareTo(sma10) > 0 && obv.compareTo(prevObv) > 0;
        boolean shortEntry = isNewBrickToday && isDownBrick && lastBrick.getBrickLow().compareTo(sma10) < 0 && obv.compareTo(prevObv) < 0;

        if (currentDayPosition == LONG) {
            // Trailing SL logic
            if (isNewBrickToday && isUpBrick) {
                if (renkoBricks.size() >= 3) {
                    BigDecimal newSL = renkoBricks.get(renkoBricks.size() - 3).getBrickLow();
                    BigDecimal currentSL = (BigDecimal) strategyState.get("trailingSL");
                    if (currentSL == null || newSL.compareTo(currentSL) > 0) {
                        strategyState.put("trailingSL", newSL);
                    }
                }
            }

            BigDecimal currentSL = (BigDecimal) strategyState.get("trailingSL");
            if (currentSL != null && currentDayMarketData.getPriceClose().compareTo(currentSL) < 0) {
                strategyState.remove("trailingSL");
                return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
            }

            if (currentDayMarketData.getPriceClose().compareTo(sma10) < 0) {
                strategyState.remove("trailingSL");
                return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
            }

            if (shortEntry) {
                strategyState.remove("trailingSL");
                if (renkoBricks.size() >= 3) {
                    strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickHigh());
                }
                return new TradeAction(TradeSignal.ENTER_SHORT, SHORT);
            }

            return new TradeAction(TradeSignal.HOLD, LONG);

        } else if (currentDayPosition == SHORT) {
            // Trailing SL logic
            if (isNewBrickToday && isDownBrick) {
                if (renkoBricks.size() >= 3) {
                    BigDecimal newSL = renkoBricks.get(renkoBricks.size() - 3).getBrickHigh();
                    BigDecimal currentSL = (BigDecimal) strategyState.get("trailingSL");
                    if (currentSL == null || newSL.compareTo(currentSL) < 0) {
                        strategyState.put("trailingSL", newSL);
                    }
                }
            }

            BigDecimal currentSL = (BigDecimal) strategyState.get("trailingSL");
            if (currentSL != null && currentDayMarketData.getPriceClose().compareTo(currentSL) > 0) {
                strategyState.remove("trailingSL");
                return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
            }

            if (currentDayMarketData.getPriceClose().compareTo(sma10) > 0) {
                strategyState.remove("trailingSL");
                return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
            }

            if (longEntry) {
                strategyState.remove("trailingSL");
                if (renkoBricks.size() >= 3) {
                    strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickLow());
                }
                return new TradeAction(TradeSignal.ENTER_LONG, LONG);
            }

            return new TradeAction(TradeSignal.HOLD, SHORT);
        }

        // NO POSITION
        if (longEntry) {
            if (renkoBricks.size() >= 3) {
                strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickLow());
            }
            return new TradeAction(TradeSignal.ENTER_LONG, LONG);
        }
        if (shortEntry) {
            if (renkoBricks.size() >= 3) {
                strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickHigh());
            }
            return new TradeAction(TradeSignal.ENTER_SHORT, SHORT);
        }

        return new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);
    }
}
