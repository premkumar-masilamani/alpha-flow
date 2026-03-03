package com.alphaflow.backtest.strategies.candlestick;

import com.alphaflow.backtest.entities.BacktestIndicator;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.IndicatorRole;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

public class BuyAndHoldRiskOverlayStrategy implements CandlestickStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyAndHoldRiskOverlayStrategy.class);

    private String filterIndicatorKey = "P_CLOSE_SMA_200";

    private BacktestStrategy entity;

    public BuyAndHoldRiskOverlayStrategy() {
    }

    public BuyAndHoldRiskOverlayStrategy(BacktestStrategy entity) {
        this.entity = entity;
        for (BacktestIndicator i : entity.getIndicators()) {
            if (i.getIndicatorRole() == IndicatorRole.FILTER) {
                this.filterIndicatorKey = i.getMetric() + "_" + i.getTransformation() + "_" + i.getPeriod();
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
    public TradeAction generateSignal(StrategyContext context) {
        BigDecimal priceClose = context.candleData().getPriceClose();
        BigDecimal filterValue = context.indicators().get(filterIndicatorKey);

        if (context.currentPosition() == PositionType.NONE) {
            // Enter LONG on first bar (filterValue is null) OR when price moves back above filter
            if (filterValue == null || priceClose.compareTo(filterValue) > 0) {
                log.debug("Strategy {} generating ENTER_LONG signal at {} price {} filter {}", getName(), context.candleData().getCandleDataDate(), priceClose, filterValue);
                return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG);
            }
        } else if (context.currentPosition() == PositionType.LONG) {
            // Exit to CASH if price falls below filter
            if (filterValue != null && priceClose.compareTo(filterValue) < 0) {
                log.debug("Strategy {} generating EXIT signal at {} price {} filter {}", getName(), context.candleData().getCandleDataDate(), priceClose, filterValue);
                return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
            }
        }

        return new TradeAction(TradeSignal.HOLD, context.currentPosition());
    }
}
