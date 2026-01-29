package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.infrastructure.entities.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CapitalAllInStrategy implements BacktestStrategy {

    @Override
    public String getName() {
        return "Capital Strategy All In";
    }

    @Override
    public TradeSignal generateSignal(
            MarketData currentDayMarketData,
            Map<String, BigDecimal> currentDayIndicators,
            PositionType currentDayPosition
    ) {

        BigDecimal ema5Vwap = currentDayIndicators.get("VWAP_EMA_5");
        BigDecimal ema21Vwap = currentDayIndicators.get("VWAP_EMA_21");
        BigDecimal sma5Poc = currentDayIndicators.get("C_POC_SMA_5");
        BigDecimal sma10Poc = currentDayIndicators.get("C_POC_SMA_10");
        BigDecimal capitalMomentum = currentDayIndicators.get("CAP_MOM_NONE_0");
        BigDecimal sma5BuyerRatio = currentDayIndicators.get("B_CAP_RATIO_SMA_5");

        if (ema5Vwap == null || ema21Vwap == null ||
                sma5Poc == null || sma10Poc == null ||
                capitalMomentum == null || sma5BuyerRatio == null) {
            return new TradeSignal(TradeAction.NO_SIGNAL, currentDayPosition);
        }

        BigDecimal priceClose = currentDayMarketData.getPriceClose();
        BigDecimal capitalVAH = currentDayMarketData.getCapitalVAH();
        BigDecimal capitalVAL = currentDayMarketData.getCapitalVAL();

        boolean enterLong =
                ema5Vwap.compareTo(ema21Vwap) > 0 &&
                        sma5Poc.compareTo(sma10Poc) > 0 &&
                        capitalMomentum.compareTo(BigDecimal.ZERO) > 0 &&
                        sma5BuyerRatio.compareTo(new BigDecimal("0.55")) > 0 &&
                        priceClose.compareTo(capitalVAH) > 0;

        boolean enterShort =
                ema5Vwap.compareTo(ema21Vwap) < 0 &&
                        sma5Poc.compareTo(sma10Poc) < 0 &&
                        capitalMomentum.compareTo(BigDecimal.ZERO) < 0 &&
                        sma5BuyerRatio.compareTo(new BigDecimal("0.45")) < 0 &&
                        priceClose.compareTo(capitalVAL) < 0;

        // 1. Structural hard exits
        if (currentDayPosition == PositionType.LONG && priceClose.compareTo(capitalVAL) < 0) {
            return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
        }
        if (currentDayPosition == PositionType.SHORT && priceClose.compareTo(capitalVAH) > 0) {
            return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
        }

        // 2. Entry / Flip signals
        if (enterLong) {
            return (currentDayPosition == PositionType.LONG)
                    ? new TradeSignal(TradeAction.HOLD, PositionType.LONG)
                    : new TradeSignal(TradeAction.ENTER_LONG, PositionType.LONG);
        }

        if (enterShort) {
            return (currentDayPosition == PositionType.SHORT)
                    ? new TradeSignal(TradeAction.HOLD, PositionType.SHORT)
                    : new TradeSignal(TradeAction.ENTER_SHORT, PositionType.SHORT);
        }

        // 3. If neither condition satisfies, hold current position
        return new TradeSignal(TradeAction.HOLD, currentDayPosition);
    }

}
