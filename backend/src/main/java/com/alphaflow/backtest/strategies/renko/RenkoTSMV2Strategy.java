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
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

@Component
public class RenkoTSMV2Strategy implements RenkoStrategy {

    private static final String INDICATOR_SMA_200 = "P_CLOSE_SMA_200";
    private static final String INDICATOR_OBV = "OBV_OBV_0";
    private static final String STATE_PREV_OBV = "prevObv";
    private static final BigDecimal OBV_THRESHOLD = new BigDecimal("0.05");

    @Override
    public String getName() {
        return "RenkoTSM_V2";
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
        Map<String, Object> state = context.state();

        if (renkoBricks == null || renkoBricks.size() < 3) {
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        BigDecimal priceClose = context.marketData() != null ? context.marketData().getPriceClose() : null;
        BigDecimal sma200 = indicators.get(INDICATOR_SMA_200);
        BigDecimal currentObv = indicators.get(INDICATOR_OBV);

        if (priceClose == null || sma200 == null || currentObv == null) {
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        // Rule 1: Market Regime Filter
        boolean bullishRegime = priceClose.compareTo(sma200) > 0;
        boolean bearishRegime = priceClose.compareTo(sma200) < 0;
        String regime = bullishRegime ? "bullish" : (bearishRegime ? "bearish" : "neutral");

        // Rule 2: OBV Momentum Threshold
        BigDecimal prevObv = (BigDecimal) state.get(STATE_PREV_OBV);
        state.put(STATE_PREV_OBV, currentObv);

        if (prevObv == null) {
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        if (prevObv.signum() == 0) {
            // Division by zero risk
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        BigDecimal obvMomentum = currentObv.subtract(prevObv).divide(prevObv.abs(), DB_MATH_CONTEXT);
        boolean obvBullish = obvMomentum.compareTo(OBV_THRESHOLD) > 0;
        boolean obvBearish = obvMomentum.compareTo(OBV_THRESHOLD.negate()) < 0;

        // Rule 3: 3-Brick Confirmation
        int size = renkoBricks.size();
        List<RenkoData> last3Bricks = renkoBricks.subList(size - 3, size);
        boolean last3Up = last3Bricks.stream().allMatch(b -> AppConstants.RENKO_BRICK_DIRECTION_UP.equals(b.getDirection()));
        boolean last3Down = last3Bricks.stream().allMatch(b -> AppConstants.RENKO_BRICK_DIRECTION_DOWN.equals(b.getDirection()));

        // Signal Data for logging
        Map<String, Object> signalData = new HashMap<>();
        signalData.put("SMA200", scale2(sma200));
        signalData.put("OBV", scale2(currentObv));
        signalData.put("prevOBV", scale2(prevObv));
        signalData.put("obvMomentum", scale2(obvMomentum));
        signalData.put("lastBrickDirection", renkoBricks.getLast().getDirection());
        signalData.put("regime", regime);

        // Entry Logic
        if (bullishRegime && last3Up && obvBullish && currentPosition != PositionType.LONG) {
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG, signalData);
        }

        if (bearishRegime && last3Down && obvBearish && currentPosition != PositionType.SHORT) {
            return new TradeAction(TradeSignal.ENTER_SHORT, PositionType.SHORT, signalData);
        }

        return new TradeAction(TradeSignal.HOLD, currentPosition, signalData);
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
