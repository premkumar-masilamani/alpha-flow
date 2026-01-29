package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import com.alphaflow.domain.enums.TradeSignal;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.alphaflow.domain.enums.PositionType.LONG;
import static com.alphaflow.domain.enums.PositionType.SHORT;
import static com.alphaflow.infrastructure.config.Constants.RENKO_BRICK_DIRECTION_DOWN;
import static com.alphaflow.infrastructure.config.Constants.RENKO_BRICK_DIRECTION_UP;

@Component
public class RenkoTSMStrategy implements RenkoBacktestStrategy {

    @Override
    public String getName() {
        return "Renko TSM";
    }

    @Override
    public TradeSignal generateSignal(
            List<RenkoData> renkoBricks,
            MarketData currentDayMarketData,
            Map<String, BigDecimal> currentDayIndicators,
            PositionType currentDayPosition,
            Map<String, Object> strategyState
    ) {
        if (renkoBricks.isEmpty()) {
            return new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);
        }

        RenkoData lastBrick = renkoBricks.getLast();
        BigDecimal sma10 = currentDayIndicators.get("P_CLOSE_SMA_10");
        BigDecimal obv = currentDayIndicators.get("OBV_OBV_0");
        BigDecimal prevObv = (BigDecimal) strategyState.getOrDefault("prevObv", BigDecimal.ZERO);
        strategyState.put("prevObv", obv);

        if (sma10 == null || obv == null) {
            return new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);
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
                return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
            }

            if (currentDayMarketData.getPriceClose().compareTo(sma10) < 0) {
                strategyState.remove("trailingSL");
                return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
            }

            if (shortEntry) {
                strategyState.remove("trailingSL");
                if (renkoBricks.size() >= 3) {
                    strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickHigh());
                }
                return new TradeSignal(TradeAction.ENTER_SHORT, SHORT);
            }

            return new TradeSignal(TradeAction.HOLD, LONG);

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
                return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
            }

            if (currentDayMarketData.getPriceClose().compareTo(sma10) > 0) {
                strategyState.remove("trailingSL");
                return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
            }

            if (longEntry) {
                strategyState.remove("trailingSL");
                if (renkoBricks.size() >= 3) {
                    strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickLow());
                }
                return new TradeSignal(TradeAction.ENTER_LONG, LONG);
            }

            return new TradeSignal(TradeAction.HOLD, SHORT);
        }

        // NO POSITION
        if (longEntry) {
            if (renkoBricks.size() >= 3) {
                strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickLow());
            }
            return new TradeSignal(TradeAction.ENTER_LONG, LONG);
        }
        if (shortEntry) {
            if (renkoBricks.size() >= 3) {
                strategyState.put("trailingSL", renkoBricks.get(renkoBricks.size() - 3).getBrickHigh());
            }
            return new TradeSignal(TradeAction.ENTER_SHORT, SHORT);
        }

        return new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);
    }
}
