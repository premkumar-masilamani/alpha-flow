package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import com.alphaflow.domain.enums.TradeSignal;
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
        BigDecimal capitalPOC = currentDayMarketData.getCapitalPOC();

        // ───────────────── Structural hard exit ─────────────────
        if (priceClose.compareTo(capitalVAL) < 0 &&
                currentDayPosition.name().startsWith("LONG")) {
            return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
        }

        if (priceClose.compareTo(capitalVAH) > 0 &&
                currentDayPosition.name().startsWith("SHORT")) {
            return new TradeSignal(TradeAction.EXIT, PositionType.NONE);
        }

        return switch (currentDayPosition) {

            // ───────────────── FLAT → ENTRY ─────────────────
            case NONE -> {

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

                if (enterLong) {
                    yield new TradeSignal(TradeAction.ENTER_LONG, PositionType.LONG_100);
                } else if (enterShort) {
                    yield new TradeSignal(TradeAction.ENTER_SHORT, PositionType.SHORT_100);
                } else {
                    yield new TradeSignal(TradeAction.HOLD, PositionType.NONE);
                }
            }

            // ───────────────── FULL LONG ─────────────────
            case LONG_100 -> {
                boolean reduce =
                        capitalMomentum.compareTo(BigDecimal.ZERO) < 0 ||
                                sma5BuyerRatio.compareTo(new BigDecimal("0.50")) < 0;

                yield reduce
                        ? new TradeSignal(TradeAction.REDUCE, PositionType.LONG_50)
                        : new TradeSignal(TradeAction.HOLD, PositionType.LONG_100);
            }

            // ───────────────── HALF LONG ─────────────────
            case LONG_50 -> {
                boolean reAdd =
                        capitalMomentum.compareTo(BigDecimal.ZERO) > 0 &&
                                priceClose.compareTo(capitalPOC) > 0;

                yield reAdd
                        ? new TradeSignal(TradeAction.ENTER_LONG, PositionType.LONG_100)
                        : new TradeSignal(TradeAction.HOLD, PositionType.LONG_50);
            }

            // ───────────────── FULL SHORT ─────────────────
            case SHORT_100 -> {
                boolean reduce =
                        capitalMomentum.compareTo(BigDecimal.ZERO) > 0 ||
                                sma5BuyerRatio.compareTo(new BigDecimal("0.50")) > 0;

                yield reduce
                        ? new TradeSignal(TradeAction.REDUCE, PositionType.SHORT_50)
                        : new TradeSignal(TradeAction.HOLD, PositionType.SHORT_100);
            }

            // ───────────────── HALF SHORT ─────────────────
            case SHORT_50 -> {
                boolean reAdd =
                        capitalMomentum.compareTo(BigDecimal.ZERO) < 0 &&
                                priceClose.compareTo(capitalPOC) < 0;

                yield reAdd
                        ? new TradeSignal(TradeAction.ENTER_SHORT, PositionType.SHORT_100)
                        : new TradeSignal(TradeAction.HOLD, PositionType.SHORT_50);
            }

            // ───────────────── Other sizes / future ─────────────────
            default -> new TradeSignal(TradeAction.HOLD, currentDayPosition);
        };
    }

}
