package com.alphaflow.backtest.strategies.candlestick;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.entities.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuyAndHoldRiskOverlayStrategyTest {

    private BuyAndHoldRiskOverlayStrategy strategy;
    private static final String SMA_200_KEY = "P_CLOSE_SMA_200";

    @BeforeEach
    void setUp() {
        strategy = new BuyAndHoldRiskOverlayStrategy();
    }

    @Test
    void shouldEnterLongOnFirstBarWhenSmaIsNull() {
        MarketData marketData = MarketData.builder()
                .priceClose(new BigDecimal("100"))
                .build();
        StrategyContext context = new StrategyContext(
                marketData,
                new HashMap<>(), // No indicators
                PositionType.NONE,
                null,
                null
        );

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.ENTER_LONG, action.tradeSignal());
        assertEquals(PositionType.LONG, action.positionType());
    }

    @Test
    void shouldEnterLongWhenPriceAboveSma200AndPositionNone() {
        MarketData marketData = MarketData.builder()
                .priceClose(new BigDecimal("110"))
                .build();
        Map<String, BigDecimal> indicators = new HashMap<>();
        indicators.put(SMA_200_KEY, new BigDecimal("100"));

        StrategyContext context = new StrategyContext(
                marketData,
                indicators,
                PositionType.NONE,
                null,
                null
        );

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.ENTER_LONG, action.tradeSignal());
        assertEquals(PositionType.LONG, action.positionType());
    }

    @Test
    void shouldHoldWhenPriceAboveSma200AndAlreadyLong() {
        MarketData marketData = MarketData.builder()
                .priceClose(new BigDecimal("110"))
                .build();
        Map<String, BigDecimal> indicators = new HashMap<>();
        indicators.put(SMA_200_KEY, new BigDecimal("100"));

        StrategyContext context = new StrategyContext(
                marketData,
                indicators,
                PositionType.LONG,
                null,
                null
        );

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.HOLD, action.tradeSignal());
        assertEquals(PositionType.LONG, action.positionType());
    }

    @Test
    void shouldExitWhenPriceFallsBelowSma200() {
        MarketData marketData = MarketData.builder()
                .priceClose(new BigDecimal("90"))
                .build();
        Map<String, BigDecimal> indicators = new HashMap<>();
        indicators.put(SMA_200_KEY, new BigDecimal("100"));

        StrategyContext context = new StrategyContext(
                marketData,
                indicators,
                PositionType.LONG,
                null,
                null
        );

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.EXIT, action.tradeSignal());
        assertEquals(PositionType.NONE, action.positionType());
    }

    @Test
    void shouldStayInCashWhenPriceBelowSma200AndPositionNone() {
        MarketData marketData = MarketData.builder()
                .priceClose(new BigDecimal("90"))
                .build();
        Map<String, BigDecimal> indicators = new HashMap<>();
        indicators.put(SMA_200_KEY, new BigDecimal("100"));

        StrategyContext context = new StrategyContext(
                marketData,
                indicators,
                PositionType.NONE,
                null,
                null
        );

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.HOLD, action.tradeSignal());
        assertEquals(PositionType.NONE, action.positionType());
    }

    @Test
    void shouldReEnterLongWhenPriceMovesBackAboveSma200() {
        MarketData marketData = MarketData.builder()
                .priceClose(new BigDecimal("105"))
                .build();
        Map<String, BigDecimal> indicators = new HashMap<>();
        indicators.put(SMA_200_KEY, new BigDecimal("100"));

        StrategyContext context = new StrategyContext(
                marketData,
                indicators,
                PositionType.NONE,
                null,
                null
        );

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.ENTER_LONG, action.tradeSignal());
        assertEquals(PositionType.LONG, action.positionType());
    }
}
