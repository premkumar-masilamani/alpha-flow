package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.entities.BacktestIndicator;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.IndicatorRole;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.constants.AppConstants;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;
import static com.alphaflow.infrastructure.utils.NumberUtils.convertToBigDecimal;
import static com.alphaflow.infrastructure.utils.NumberUtils.scale2;

@Component
public class RenkoTSMV2Strategy implements RenkoStrategy {

    private static final Logger log = LoggerFactory.getLogger(RenkoTSMV2Strategy.class);
    private static final BigDecimal OBV_THRESHOLD = new BigDecimal("0.05");
    private String filterIndicatorKey = "P_CLOSE_SMA_200";
    private String momentumIndicatorKey = "OBV_OBV_0";
    private String prevMomentumIndicatorKey = "PREV_OBV_OBV_0";
    private BacktestStrategy entity;

    public RenkoTSMV2Strategy() {
    }

    public RenkoTSMV2Strategy(BacktestStrategy entity) {
        this.entity = entity;
        for (BacktestIndicator i : entity.getIndicators()) {
            if (i.getIndicatorRole() == IndicatorRole.FILTER) {
                this.filterIndicatorKey = i.getMetric() + "_" + i.getTransformation() + "_" + i.getPeriod();
            } else if (i.getIndicatorRole() == IndicatorRole.MOMENTUM) {
                this.momentumIndicatorKey = i.getMetric() + "_" + i.getTransformation() + "_" + i.getPeriod();
                this.prevMomentumIndicatorKey = "PREV_" + this.momentumIndicatorKey;
            }
        }
    }

    @Override
    public String getName() {
        return entity != null ? entity.getName() : "RenkoData TSM V2";
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
    public TradeAction generateSignal(StrategyContext context) {
        List<RenkoData> renkoData = context.renkoData();
        Map<String, BigDecimal> indicators = context.indicators();
        PositionType currentPosition = context.currentPosition();
        Map<String, Object> state = context.state();

        if (renkoData == null || renkoData.size() < 3) {
            return new TradeAction(TradeSignal.HOLD, currentPosition);
        }

        BigDecimal priceClose = context.candleData() != null ? context.candleData().getPriceClose() : null;
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
        BigDecimal prevObv = convertToBigDecimal(state.get(prevMomentumIndicatorKey));
        state.put(prevMomentumIndicatorKey, currentObv);

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
        int size = renkoData.size();
        List<RenkoData> last3Bricks = renkoData.subList(size - 3, size);
        boolean last3Up = last3Bricks.stream().allMatch(b -> AppConstants.RENKO_BRICK_DIRECTION_UP.equals(b.getDirection()));
        boolean last3Down = last3Bricks.stream().allMatch(b -> AppConstants.RENKO_BRICK_DIRECTION_DOWN.equals(b.getDirection()));

        // Signal Data for logging
        Map<String, Object> signalData = new HashMap<>();
        signalData.put("filter", scale2(filterValue));
        signalData.put("OBV", scale2(currentObv));
        signalData.put("prevOBV", scale2(prevObv));
        signalData.put("obvMomentum", scale2(obvMomentum));
        signalData.put("direction", renkoData.getLast().getDirection());
        signalData.put("regime", regime);

        // Entry Logic
        if (bullishRegime && last3Up && obvBullish && currentPosition != PositionType.LONG) {
            log.debug("Strategy {} generating ENTER_LONG signal at {}", getName(), context.candleData().getCandleDataDate());
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG, signalData);
        }

        if (bearishRegime && last3Down && obvBearish && currentPosition != PositionType.SHORT) {
            log.debug("Strategy {} generating ENTER_SHORT signal at {}", getName(), context.candleData().getCandleDataDate());
            return new TradeAction(TradeSignal.ENTER_SHORT, PositionType.SHORT, signalData);
        }

        return new TradeAction(TradeSignal.HOLD, currentPosition, signalData);
    }
}
