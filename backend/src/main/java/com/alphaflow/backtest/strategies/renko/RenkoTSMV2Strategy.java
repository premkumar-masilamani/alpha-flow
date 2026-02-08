package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestStrategyIndicator;
import com.alphaflow.backtest.enums.IndicatorRole;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.indicators.IndicatorKey;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.backtest.strategies.StrategyState;
import com.alphaflow.infrastructure.constants.AppConstants;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

@Component
public class RenkoTSMV2Strategy implements RenkoStrategy<RenkoTSMV2Strategy.State> {

    private static final Logger log = LoggerFactory.getLogger(RenkoTSMV2Strategy.class);
    private static final BigDecimal OBV_THRESHOLD = new BigDecimal("0.05");
    private IndicatorKey filterIndicatorKey = new IndicatorKey("P_CLOSE", "SMA", 200);
    private IndicatorKey momentumIndicatorKey = new IndicatorKey("OBV", "OBV", 0);
    private BacktestStrategy entity;

    public RenkoTSMV2Strategy() {
    }

    public RenkoTSMV2Strategy(BacktestStrategy entity) {
        this.entity = entity;
        for (BacktestStrategyIndicator i : entity.getIndicators()) {
            if (i.getIndicatorRole() == IndicatorRole.FILTER) {
                this.filterIndicatorKey = new IndicatorKey(i.getMetric(), i.getTransformation(), i.getPeriod());
            } else if (i.getIndicatorRole() == IndicatorRole.MOMENTUM) {
                this.momentumIndicatorKey = new IndicatorKey(i.getMetric(), i.getTransformation(), i.getPeriod());
            }
        }
    }

    @Override
    public String getName() {
        return entity != null ? entity.getName() : "Renko TSM V2";
    }

    @Override
    public BacktestStrategy getEntity() {
        return entity;
    }

    @Override
    public RenkoPriceSource getPriceSource() {
        return RenkoPriceSource.PRICE_CLOSE;
    }

    @Override
    public State initialState() {
        return new State();
    }

    @Override
    public TradeAction generateSignal(StrategyContext<State> context) {
        List<RenkoData> renkoBricks = context.renkoBricks();
        Map<IndicatorKey, BigDecimal> indicators = context.indicators();
        PositionType currentPosition = context.currentPosition();
        State state = context.state();

        if (renkoBricks == null || renkoBricks.size() < 3) {
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        BigDecimal priceClose = context.marketData() != null ? context.marketData().getPriceClose() : null;
        BigDecimal filterValue = indicators.get(filterIndicatorKey);
        BigDecimal currentObv = indicators.get(momentumIndicatorKey);

        if (priceClose == null || filterValue == null || currentObv == null) {
            log.debug("Strategy {} missing indicators: priceClose={}, filter={}, obv={}", getName(), priceClose, filterValue, currentObv);
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        // Rule 1: Market Regime Filter
        boolean bullishRegime = priceClose.compareTo(filterValue) > 0;
        boolean bearishRegime = priceClose.compareTo(filterValue) < 0;
        String regime = bullishRegime ? "bullish" : (bearishRegime ? "bearish" : "neutral");

        // Rule 2: OBV Momentum Threshold
        BigDecimal prevObv = state.getPreviousMomentum();
        state.setPreviousMomentum(currentObv);

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
        signalData.put("filter", scale2(filterValue));
        signalData.put("OBV", scale2(currentObv));
        signalData.put("prevOBV", scale2(prevObv));
        signalData.put("obvMomentum", scale2(obvMomentum));
        signalData.put("direction", renkoBricks.getLast().getDirection());
        signalData.put("regime", regime);

        // Entry Logic
        if (bullishRegime && last3Up && obvBullish && currentPosition != PositionType.LONG) {
            log.debug("Strategy {} generating ENTER_LONG signal at {}", getName(), context.marketData().getMarketDataDate());
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG, signalData);
        }

        if (bearishRegime && last3Down && obvBearish && currentPosition != PositionType.SHORT) {
            log.debug("Strategy {} generating ENTER_SHORT signal at {}", getName(), context.marketData().getMarketDataDate());
            return new TradeAction(TradeSignal.ENTER_SHORT, PositionType.SHORT, signalData);
        }

        return new TradeAction(TradeSignal.HOLD, currentPosition, signalData);
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static final class State implements StrategyState {
        private BigDecimal previousMomentum;

        public BigDecimal getPreviousMomentum() {
            return previousMomentum;
        }

        public void setPreviousMomentum(BigDecimal previousMomentum) {
            this.previousMomentum = previousMomentum;
        }
    }
}
