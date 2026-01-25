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

        // CASE structure based on SQL logic
        // LONG ENTRY
        if (ema5Vwap.compareTo(ema21Vwap) > 0 &&
                sma5Poc.compareTo(sma10Poc) > 0 &&
                capitalMomentum.compareTo(BigDecimal.ZERO) > 0 &&
                sma5BuyerRatio.compareTo(new BigDecimal("0.55")) > 0 &&
                priceClose.compareTo(capitalVah) > 0) {
            return BacktestSignal.LONG_ENTRY;
        }

        // SHORT ENTRY
        if (ema5Vwap.compareTo(ema21Vwap) < 0 &&
                sma5Poc.compareTo(sma10Poc) < 0 &&
                capitalMomentum.compareTo(BigDecimal.ZERO) < 0 &&
                sma5BuyerRatio.compareTo(new BigDecimal("0.45")) < 0 &&
                priceClose.compareTo(capitalVal) < 0) {
            return BacktestSignal.SHORT_ENTRY;
        }

        // EXIT (LONG)
        if (currentPosition == PositionType.LONG) {
            if (capitalMomentum.compareTo(BigDecimal.ZERO) <= 0 ||
                    priceClose.compareTo(capitalVah) <= 0 ||
                    sma5BuyerRatio.compareTo(new BigDecimal("0.50")) < 0) {
                return BacktestSignal.EXIT_LONG;
            }
        }

        // EXIT (SHORT)
        if (currentPosition == PositionType.SHORT) {
            if (capitalMomentum.compareTo(BigDecimal.ZERO) >= 0 ||
                    priceClose.compareTo(capitalVal) >= 0 ||
                    sma5BuyerRatio.compareTo(new BigDecimal("0.50")) > 0) {
                return BacktestSignal.EXIT_SHORT;
            }
        }

        return BacktestSignal.HOLD;
    }
}
