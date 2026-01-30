package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.constants.AppConstants;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.RenkoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenkoTSMV2StrategyTest {

    private RenkoTSMV2Strategy strategy;
    private Map<String, Object> state;
    private Map<String, BigDecimal> indicators;
    private List<RenkoData> renkoBricks;

    @BeforeEach
    void setUp() {
        strategy = new RenkoTSMV2Strategy();
        state = new HashMap<>();
        indicators = new HashMap<>();
        renkoBricks = new ArrayList<>();
    }

    @Test
    void shouldReturnHoldWhenFewerThan3Bricks() {
        renkoBricks.add(createBrick(AppConstants.RENKO_BRICK_DIRECTION_UP));
        renkoBricks.add(createBrick(AppConstants.RENKO_BRICK_DIRECTION_UP));
        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    @Test
    void shouldReturnHoldWhenIndicatorsMissing() {
        add3Bricks(AppConstants.RENKO_BRICK_DIRECTION_UP);
        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    @Test
    void shouldEnterLongWhenAllConditionsMet() {
        add3Bricks(AppConstants.RENKO_BRICK_DIRECTION_UP);
        indicators.put("P_CLOSE_SMA_200", new BigDecimal("90")); // Bullish regime (100 > 90)
        indicators.put("OBV_OBV_0", new BigDecimal("1100"));
        state.put("prevObv", new BigDecimal("1000")); // Momentum = (1100-1000)/1000 = 0.1 > 0.05

        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.ENTER_LONG, action.tradeSignal());
        assertEquals(PositionType.LONG, action.positionType());
    }

    @Test
    void shouldNotEnterLongInBearishRegime() {
        add3Bricks(AppConstants.RENKO_BRICK_DIRECTION_UP);
        indicators.put("P_CLOSE_SMA_200", new BigDecimal("110")); // Bearish regime (100 < 110)
        indicators.put("OBV_OBV_0", new BigDecimal("1100"));
        state.put("prevObv", new BigDecimal("1000"));

        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    @Test
    void shouldEnterShortWhenAllConditionsMet() {
        add3Bricks(AppConstants.RENKO_BRICK_DIRECTION_DOWN);
        indicators.put("P_CLOSE_SMA_200", new BigDecimal("110")); // Bearish regime (100 < 110)
        indicators.put("OBV_OBV_0", new BigDecimal("900"));
        state.put("prevObv", new BigDecimal("1000")); // Momentum = (900-1000)/1000 = -0.1 < -0.05

        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.ENTER_SHORT, action.tradeSignal());
        assertEquals(PositionType.SHORT, action.positionType());
    }

    @Test
    void shouldNotEnterShortInBullishRegime() {
        add3Bricks(AppConstants.RENKO_BRICK_DIRECTION_DOWN);
        indicators.put("P_CLOSE_SMA_200", new BigDecimal("90")); // Bullish regime (100 > 90)
        indicators.put("OBV_OBV_0", new BigDecimal("900"));
        state.put("prevObv", new BigDecimal("1000"));

        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    @Test
    void shouldReturnHoldWhenObvMomentumInsufficient() {
        add3Bricks(AppConstants.RENKO_BRICK_DIRECTION_UP);
        indicators.put("P_CLOSE_SMA_200", new BigDecimal("90"));
        indicators.put("OBV_OBV_0", new BigDecimal("1040"));
        state.put("prevObv", new BigDecimal("1000")); // Momentum = 0.04 < 0.05

        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    @Test
    void shouldReturnHoldWhenBricksMixed() {
        renkoBricks.add(createBrick(AppConstants.RENKO_BRICK_DIRECTION_UP));
        renkoBricks.add(createBrick(AppConstants.RENKO_BRICK_DIRECTION_DOWN));
        renkoBricks.add(createBrick(AppConstants.RENKO_BRICK_DIRECTION_UP));
        indicators.put("P_CLOSE_SMA_200", new BigDecimal("90"));
        indicators.put("OBV_OBV_0", new BigDecimal("1100"));
        state.put("prevObv", new BigDecimal("1000"));

        StrategyContext context = new StrategyContext(createMarketData("100"), indicators, PositionType.NONE, renkoBricks, state);

        TradeAction action = strategy.generateSignal(context);
        assertEquals(TradeSignal.HOLD, action.tradeSignal());
    }

    private RenkoData createBrick(String direction) {
        return RenkoData.builder().direction(direction).build();
    }

    private void add3Bricks(String direction) {
        renkoBricks.add(createBrick(direction));
        renkoBricks.add(createBrick(direction));
        renkoBricks.add(createBrick(direction));
    }

    private MarketData createMarketData(String priceClose) {
        return MarketData.builder().priceClose(new BigDecimal(priceClose)).build();
    }
}
