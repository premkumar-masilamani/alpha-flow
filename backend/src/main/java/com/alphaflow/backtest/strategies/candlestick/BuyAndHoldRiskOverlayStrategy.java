package com.alphaflow.backtest.strategies.candlestick;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BuyAndHoldRiskOverlayStrategy implements CandlestickStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyAndHoldRiskOverlayStrategy.class);

    private static final String SMA_200_KEY = "P_CLOSE_SMA_200";

    private BacktestStrategy entity;

    public BuyAndHoldRiskOverlayStrategy() {
    }

    public BuyAndHoldRiskOverlayStrategy(BacktestStrategy entity) {
        this.entity = entity;
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
        BigDecimal priceClose = context.marketData().getPriceClose();
        BigDecimal sma200 = context.indicators().get(SMA_200_KEY);

        if (context.currentPosition() == PositionType.NONE) {
            // Enter LONG on first bar (sma200 is null) OR when price moves back above SMA 200
            if (sma200 == null || priceClose.compareTo(sma200) > 0) {
                log.debug("Strategy {} generating ENTER_LONG signal at {} price {} SMA200 {}", getName(), context.marketData().getMarketDataDate(), priceClose, sma200);
                return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG);
            }
        } else if (context.currentPosition() == PositionType.LONG) {
            // Exit to CASH if price falls below SMA 200
            if (sma200 != null && priceClose.compareTo(sma200) < 0) {
                log.debug("Strategy {} generating EXIT signal at {} price {} SMA200 {}", getName(), context.marketData().getMarketDataDate(), priceClose, sma200);
                return new TradeAction(TradeSignal.EXIT, PositionType.NONE);
            }
        }

        return new TradeAction(TradeSignal.HOLD, context.currentPosition());
    }
}
