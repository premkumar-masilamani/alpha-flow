package com.alphaflow.backtest.strategies.candlestick;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestStrategyIndicator;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BuyAndHoldRiskOverlayStrategy implements CandlestickStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyAndHoldRiskOverlayStrategy.class);

    private static final String SMA_200_KEY = "P_CLOSE_SMA_200";

    @Override
    public String getName() {
        return "Buy & Hold with Risk Overlay";
    }

    @Override
    public BacktestStrategy getEntity() {
        BacktestStrategy strategy = BacktestStrategy.builder()
                .name(getName())
                .strategyType("CANDLESTICK_B&H_RISK")
                .build();

        List<BacktestStrategyIndicator> indicators = List.of(
                BacktestStrategyIndicator.builder()
                        .backtestStrategy(strategy)
                        .indicatorRole("RISK_OVERLAY")
                        .metric("P_CLOSE")
                        .transformation("SMA")
                        .period(200)
                        .build()
        );

        strategy.setIndicators(indicators);
        return strategy;
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
