package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.constants.AppConstants;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RenkoTSMStrategy implements RenkoStrategy {

    private static final String INDICATOR_PRICE_CLOSE_SMA_10 = "P_CLOSE_SMA_10";
    private static final String INDICATOR_OBV = "OBV_OBV_0";
    private static final String INDICATOR_PREVIOUS_OBV = "PREV_OBV_OBV_0";

    @Override
    public String getName() {
        return "Renko TSM";
    }

    @Override
    public RenkoPriceSource getPriceSource() {
        return RenkoPriceSource.PRICE_CLOSE;
    }

    @Override
    public TradeAction generateSignal(StrategyContext context) {

        List<RenkoData> renkoBricks = context.renkoBricks();
        Map<String, BigDecimal> indicators = context.indicators();
        PositionType currentPosition = context.currentPosition();
        Map<String, Object> strategyState = context.state();

        if (renkoBricks == null || renkoBricks.isEmpty())
            return new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);

        RenkoData lastBrick = renkoBricks.getLast();

        BigDecimal priceCloseSma10 = indicators.get(INDICATOR_PRICE_CLOSE_SMA_10);
        BigDecimal obv = indicators.get(INDICATOR_OBV);
        BigDecimal prevObv = (BigDecimal) strategyState.getOrDefault(INDICATOR_PREVIOUS_OBV, obv);
        strategyState.put(INDICATOR_PREVIOUS_OBV, obv);

        if (priceCloseSma10 == null || obv == null)
            return new TradeAction(TradeSignal.NO_SIGNAL, currentPosition);

        boolean longEntry = lastBrick.getDirection().equals(AppConstants.RENKO_BRICK_DIRECTION_UP) &&
                lastBrick.getBrickHigh().compareTo(priceCloseSma10) > 0 &&
                obv.compareTo(prevObv) > 0;

        boolean shortEntry = lastBrick.getDirection().equals(AppConstants.RENKO_BRICK_DIRECTION_DOWN) &&
                lastBrick.getBrickLow().compareTo(priceCloseSma10) < 0 &&
                obv.compareTo(prevObv) < 0;

        Map<String, Object> signalData = new HashMap<>();
        signalData.put(INDICATOR_PRICE_CLOSE_SMA_10, priceCloseSma10);
        signalData.put(INDICATOR_OBV, obv);
        signalData.put(INDICATOR_PREVIOUS_OBV, prevObv);
        signalData.put("lastBrickHigh", lastBrick.getBrickHigh());
        signalData.put("lastBrickLow", lastBrick.getBrickLow());
        signalData.put("lastBrickDirection", lastBrick.getDirection());

        if ((currentPosition == PositionType.NONE || currentPosition == PositionType.LONG) && shortEntry) {
            return new TradeAction(TradeSignal.ENTER_SHORT, PositionType.SHORT, signalData);
        }

        if ((currentPosition == PositionType.NONE || currentPosition == PositionType.SHORT) && longEntry) {
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG, signalData);
        }

        return new TradeAction(TradeSignal.HOLD, currentPosition, signalData);
    }
}
