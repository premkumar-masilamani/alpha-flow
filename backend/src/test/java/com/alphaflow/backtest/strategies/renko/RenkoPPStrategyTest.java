package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.RenkoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphaflow.infrastructure.constants.AppConstants.RENKO_BRICK_DIRECTION_DOWN;
import static com.alphaflow.infrastructure.constants.AppConstants.RENKO_BRICK_DIRECTION_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RenkoPPStrategyTest {

    private RenkoPPStrategy strategy;
    private Map<String, BigDecimal> indicators;
    private Map<String, Object> state;
    private List<RenkoData> renkoBricks;

    @BeforeEach
    void setUp() {
        strategy = new RenkoPPStrategy();
        indicators = new HashMap<>();
        state = new HashMap<>();
        renkoBricks = new ArrayList<>();

        // Set up GMMA EMAs (Bullish order)
        indicators.put("P_CLOSE_EMA_3", new BigDecimal("100"));
        indicators.put("P_CLOSE_EMA_5", new BigDecimal("98"));
        indicators.put("P_CLOSE_EMA_8", new BigDecimal("96"));
        indicators.put("P_CLOSE_EMA_10", new BigDecimal("94"));
        indicators.put("P_CLOSE_EMA_12", new BigDecimal("92"));
        indicators.put("P_CLOSE_EMA_15", new BigDecimal("90"));
        indicators.put("P_CLOSE_EMA_30", new BigDecimal("80"));
        indicators.put("P_CLOSE_EMA_35", new BigDecimal("78"));
        indicators.put("P_CLOSE_EMA_40", new BigDecimal("76"));
        indicators.put("P_CLOSE_EMA_45", new BigDecimal("74"));
        indicators.put("P_CLOSE_EMA_50", new BigDecimal("72"));
        indicators.put("P_CLOSE_EMA_60", new BigDecimal("70"));

        // Set up initial state (Expansion)
        state.put("PREV_SHORT_SPREAD_LONG", new BigDecimal("5")); // current is 100-90=10
        state.put("PREV_LONG_SPREAD_LONG", new BigDecimal("5"));  // current is 80-70=10
        state.put("PREV_SHORT_SPREAD_SHORT", new BigDecimal("5"));
        state.put("PREV_LONG_SPREAD_SHORT", new BigDecimal("5"));
    }

    @Test
    void shouldEnterLongWhenConditionsMet() {
        // Renko condition: 3 Red bricks followed by 4 Green bricks (Brick 4 of Zone 1)
        for (int i = 1; i <= 3; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_DOWN).trend(i).zone(0).build());
        }
        for (int i = 1; i <= 4; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(i).zone(i <= 3 ? 0 : 1).build());
        }

        MarketData marketData = MarketData.builder().priceClose(new BigDecimal("105")).build();
        StrategyContext context = new StrategyContext(marketData, indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.ENTER_LONG, action.tradeSignal());
        assertEquals(PositionType.LONG, action.positionType());
    }

    @Test
    void shouldExitLongOnStopLoss() {
        // Brick 4, Zone 1. SL Index = 4-1-1 = 2.
        // Brick 2 Low will be SL.
        renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(1).zone(0).brickLow(new BigDecimal("90")).build());
        renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(2).zone(0).brickLow(new BigDecimal("95")).build());
        renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(3).zone(0).brickLow(new BigDecimal("100")).build());
        renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(4).zone(1).brickLow(new BigDecimal("105")).build());

        // Price drops below Brick 2 Low (95)
        MarketData marketData = MarketData.builder().priceClose(new BigDecimal("94")).build();
        StrategyContext context = new StrategyContext(marketData, indicators, PositionType.LONG, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.EXIT, action.tradeSignal());
        assertEquals(PositionType.NONE, action.positionType());
    }

    @Test
    void shouldNotEnterLongIfPreviousTrendTooShort() {
        // Only 2 Red bricks before
        for (int i = 1; i <= 2; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_DOWN).trend(i).zone(0).build());
        }
        for (int i = 1; i <= 4; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(i).zone(i <= 3 ? 0 : 1).build());
        }

        MarketData marketData = MarketData.builder().priceClose(new BigDecimal("105")).build();
        StrategyContext context = new StrategyContext(marketData, indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    @Test
    void shouldNotEnterLongIfNoExpansion() {
        // Previous spreads were larger than current
        state.put("PREV_SHORT_SPREAD_LONG", new BigDecimal("15")); // current is 10

        for (int i = 1; i <= 3; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_DOWN).trend(i).zone(0).build());
        }
        for (int i = 1; i <= 4; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(i).zone(i <= 3 ? 0 : 1).build());
        }

        MarketData marketData = MarketData.builder().priceClose(new BigDecimal("105")).build();
        StrategyContext context = new StrategyContext(marketData, indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    @Test
    void shouldEnterShortWhenConditionsMet() {
        // Bearish order EMAs
        indicators.put("P_CLOSE_EMA_3", new BigDecimal("70"));
        indicators.put("P_CLOSE_EMA_60", new BigDecimal("100"));
        indicators.put("P_CLOSE_EMA_15", new BigDecimal("80"));
        indicators.put("P_CLOSE_EMA_30", new BigDecimal("90"));
        // ... more for strict order check
        indicators.put("P_CLOSE_EMA_5", new BigDecimal("72"));
        indicators.put("P_CLOSE_EMA_8", new BigDecimal("74"));
        indicators.put("P_CLOSE_EMA_10", new BigDecimal("76"));
        indicators.put("P_CLOSE_EMA_12", new BigDecimal("78"));
        indicators.put("P_CLOSE_EMA_35", new BigDecimal("92"));
        indicators.put("P_CLOSE_EMA_40", new BigDecimal("94"));
        indicators.put("P_CLOSE_EMA_45", new BigDecimal("96"));
        indicators.put("P_CLOSE_EMA_50", new BigDecimal("98"));

        // Expansion: current shortSpreadShort is 80-70=10. Prev was 5.
        state.put("PREV_SHORT_SPREAD_SHORT", new BigDecimal("5"));
        state.put("PREV_LONG_SPREAD_SHORT", new BigDecimal("5"));

        // Renko condition: 3 Green bricks followed by 4 Red bricks
        for (int i = 1; i <= 3; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_UP).trend(i).zone(0).build());
        }
        for (int i = 1; i <= 4; i++) {
            renkoBricks.add(RenkoData.builder().direction(RENKO_BRICK_DIRECTION_DOWN).trend(i).zone(i <= 3 ? 0 : 1).build());
        }

        MarketData marketData = MarketData.builder().priceClose(new BigDecimal("65")).build();
        StrategyContext context = new StrategyContext(marketData, indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);

        assertEquals(TradeSignal.ENTER_SHORT, action.tradeSignal());
        assertEquals(PositionType.SHORT, action.positionType());
    }
}
