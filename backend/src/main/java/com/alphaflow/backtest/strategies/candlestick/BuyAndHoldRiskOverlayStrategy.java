package com.alphaflow.backtest.strategies.candlestick;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestStrategyIndicator;
import com.alphaflow.backtest.enums.IndicatorRole;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.indicators.IndicatorKey;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.backtest.strategies.StrategyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BuyAndHoldRiskOverlayStrategy implements CandlestickStrategy<BuyAndHoldRiskOverlayStrategy.State> {

    private static final Logger log = LoggerFactory.getLogger(BuyAndHoldRiskOverlayStrategy.class);

    private IndicatorKey filterIndicatorKey = new IndicatorKey("P_CLOSE", "SMA", 200);

    private BacktestStrategy entity;

    public BuyAndHoldRiskOverlayStrategy() {
    }

    public BuyAndHoldRiskOverlayStrategy(BacktestStrategy entity) {
        this.entity = entity;
        for (BacktestStrategyIndicator i : entity.getIndicators()) {
            if (i.getIndicatorRole() == IndicatorRole.FILTER) {
                this.filterIndicatorKey = new IndicatorKey(i.getMetric(), i.getTransformation(), i.getPeriod());
            }
        }
    }

    @Override
    public String getName() {
        return entity != null ? entity.getName() : "Buy & Hold with Risk Overlay";
    }

    @Override
    public BacktestStrategy getEntity() {
        return entity;
    }

    @Override
    public State initialState() {
        return new State();
    }

    @Override
    public TradeAction generateSignal(StrategyContext<State> context) {
        BigDecimal priceClose = context.marketData().getPriceClose();
        BigDecimal filterValue = context.indicators().get(filterIndicatorKey);

        if (context.currentPosition() == PositionType.NONE) {
            // Enter LONG on first bar (filterValue is null) OR when price moves back above filter
            if (filterValue == null || priceClose.compareTo(filterValue) > 0) {
                log.debug("Strategy {} generating ENTER_LONG signal at {} price {} filter {}", getName(), context.marketData().getMarketDataDate(), priceClose, filterValue);
                return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG);
            }
        } else if (context.currentPosition() == PositionType.LONG) {
            // Exit to CASH if price falls below filter
            if (filterValue != null && priceClose.compareTo(filterValue) < 0) {
                log.debug("Strategy {} generating EXIT signal at {} price {} filter {}", getName(), context.marketData().getMarketDataDate(), priceClose, filterValue);
                return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
            }
        }

        return new TradeAction(TradeSignal.HOLD, context.currentPosition());
    }

    public static final class State implements StrategyState {
    }
}
