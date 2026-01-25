package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.BacktestSignal;
import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CapitalStrategy implements BacktestStrategy {

    @Override
    public String getName() {
        return "Capital Strategy";
    }

    @Override
    public BacktestSignal generateSignal(MarketData marketData, Map<String, BigDecimal> indicators, PositionType currentPosition) {
        BigDecimal ema5Vwap = indicators.get("VWAP_EMA_5");
        BigDecimal ema21Vwap = indicators.get("VWAP_EMA_21");
        BigDecimal sma5Poc = indicators.get("C_POC_SMA_5");
        BigDecimal sma10Poc = indicators.get("C_POC_SMA_10");
        BigDecimal capitalMomentum = indicators.get("CAP_MOM_NONE_0");
        BigDecimal sma5BuyerRatio = indicators.get("B_CAP_RATIO_SMA_5");

        if (ema5Vwap == null || ema21Vwap == null || sma5Poc == null || sma10Poc == null ||
                capitalMomentum == null || sma5BuyerRatio == null) {
            return BacktestSignal.NONE;
        }

        BigDecimal priceClose = marketData.getPriceClose();
        BigDecimal capitalVah = marketData.getCapitalVAH();
        BigDecimal capitalVal = marketData.getCapitalVAL();
        BigDecimal capitalPoc = marketData.getCapitalPOC();

        // FULL EXIT (0%) — Structural Failure Only
        if (priceClose.compareTo(capitalVal) < 0) {
            return BacktestSignal.GO_NONE;
        }

        if (currentPosition == PositionType.NONE) {
            // ENTRY RULE (100% position)
            if (ema5Vwap.compareTo(ema21Vwap) > 0 &&
                    sma5Poc.compareTo(sma10Poc) > 0 &&
                    capitalMomentum.compareTo(BigDecimal.ZERO) > 0 &&
                    sma5BuyerRatio.compareTo(new BigDecimal("0.55")) > 0 &&
                    priceClose.compareTo(capitalVah) > 0) {
                return BacktestSignal.GO_LONG_100;
            }
        } else if (currentPosition == PositionType.LONG_100) {
            // Reduce to 50% (warning state)
            if (capitalMomentum.compareTo(BigDecimal.ZERO) < 0 ||
                    sma5BuyerRatio.compareTo(new BigDecimal("0.50")) < 0) {
                return BacktestSignal.GO_LONG_50;
            }
        } else if (currentPosition == PositionType.LONG_50) {
            // Re-add from 50% → 100%
            if (capitalMomentum.compareTo(BigDecimal.ZERO) > 0 &&
                    priceClose.compareTo(capitalPoc) > 0) {
                return BacktestSignal.GO_LONG_100;
            }
        }

        return BacktestSignal.HOLD;
    }
}
