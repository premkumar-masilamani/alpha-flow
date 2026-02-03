package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.engine.enums.MarketDataMetricType;
import com.alphaflow.engine.enums.TransformationType;
import com.alphaflow.engine.enums.WindowPeriod;
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

@Component
public class RenkoTSMStrategy implements RenkoStrategy {

    private static final Logger log = LoggerFactory.getLogger(RenkoTSMStrategy.class);

    private final RenkoPriceSource priceSource;
    private final TransformationType maType;
    private final WindowPeriod maPeriod;
    private final MarketDataMetricType momentumMetric;

    private final String maIndicatorKey;
    private final String momentumIndicatorKey;
    private final String momentumIndicatorPrevKey;

    public RenkoTSMStrategy() {
        this(RenkoPriceSource.PRICE_CLOSE, TransformationType.SMA, WindowPeriod.TEN_DAYS, MarketDataMetricType.OBV);
    }

    public RenkoTSMStrategy(RenkoPriceSource priceSource, TransformationType maType, WindowPeriod maPeriod, MarketDataMetricType momentumMetric) {
        this.priceSource = priceSource;
        this.maType = maType;
        this.maPeriod = maPeriod;
        this.momentumMetric = momentumMetric;

        this.maIndicatorKey = priceSource.code() + "_" + maType.code() + "_" + maPeriod.days();
        this.momentumIndicatorKey = momentumMetric.code() + "_" + momentumMetric.code() + "_0";
        this.momentumIndicatorPrevKey = "PREV_" + this.momentumIndicatorKey;
    }

    @Override
    public String getName() {
        if (isDefault()) {
            return "Renko TSM";
        }
        return String.format("Renko TSM %s %s %d %s", priceSource, maType, maPeriod.days(), momentumMetric);
    }

    private boolean isDefault() {
        return priceSource == RenkoPriceSource.PRICE_CLOSE &&
                maType == TransformationType.SMA &&
                maPeriod == WindowPeriod.TEN_DAYS &&
                momentumMetric == MarketDataMetricType.OBV;
    }

    @Override
    public RenkoPriceSource getPriceSource() {
        return priceSource;
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

        BigDecimal maValue = indicators.get(maIndicatorKey);
        BigDecimal currentMomentum = indicators.get(momentumIndicatorKey);
        BigDecimal previousMomentum = (BigDecimal) strategyState.getOrDefault(momentumIndicatorPrevKey, currentMomentum);
        strategyState.put(momentumIndicatorPrevKey, currentMomentum);

        if (maValue == null || currentMomentum == null) {
            log.debug("Missing indicators for strategy {}: {}={}, {}={}", getName(), maIndicatorKey, maValue, momentumIndicatorKey, currentMomentum);
            return new TradeAction(TradeSignal.NO_SIGNAL, currentPosition);
        }

        boolean longEntry = lastBrick.getDirection().equals(AppConstants.RENKO_BRICK_DIRECTION_UP) &&
                lastBrick.getBrickHigh().compareTo(maValue) > 0 &&
                currentMomentum.compareTo(previousMomentum) > 0;

        boolean shortEntry = lastBrick.getDirection().equals(AppConstants.RENKO_BRICK_DIRECTION_DOWN) &&
                lastBrick.getBrickLow().compareTo(maValue) < 0 &&
                currentMomentum.compareTo(previousMomentum) < 0;

        Map<String, Object> signalData = new HashMap<>();
        signalData.put(maIndicatorKey, scale2(maValue));
        signalData.put(momentumIndicatorKey, scale2(currentMomentum));
        signalData.put(momentumIndicatorPrevKey, scale2(previousMomentum));
        signalData.put("high", scale2(lastBrick.getBrickHigh()));
        signalData.put("low", scale2(lastBrick.getBrickLow()));
        signalData.put("direction", lastBrick.getDirection());

        if ((currentPosition == PositionType.NONE || currentPosition == PositionType.LONG) && shortEntry) {
            log.debug("Strategy {} generating ENTER_SHORT signal at {}", getName(), lastBrick.getRenkoDate());
            return new TradeAction(TradeSignal.ENTER_SHORT, PositionType.SHORT, signalData);
        }

        if ((currentPosition == PositionType.NONE || currentPosition == PositionType.SHORT) && longEntry) {
            log.debug("Strategy {} generating ENTER_LONG signal at {}", getName(), lastBrick.getRenkoDate());
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG, signalData);
        }

        return new TradeAction(TradeSignal.HOLD, currentPosition, signalData);
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

}
