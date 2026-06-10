package com.alphaflow.engine.strategies.evaluators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.enums.EvaluatorMessage;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvaluatorsTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);
  private static final LocalDate YESTERDAY = LocalDate.of(2026, 5, 29);
  private static final Ticker TICKER =
      Ticker.builder().tickerSymbol("AAPL").tickerName("Apple Inc.").build();

  private DailyPrice candle(LocalDate date, double open, double close, double volume) {
    return DailyPrice.builder()
        .priceDate(date)
        .priceOpen(BigDecimal.valueOf(open))
        .priceClose(BigDecimal.valueOf(close))
        .volume(BigDecimal.valueOf(volume))
        .ticker(TICKER)
        .build();
  }

  private IndicatorDefinition definition(IndicatorType type, PriceSource source, String params) {
    Map<String, Integer> paramMap = new java.util.HashMap<>();
    if (params != null && !params.isEmpty()) {
      for (String pair : params.split(",")) {
        String[] parts = pair.split("=");
        if (parts.length == 2) {
          try {
            paramMap.put(parts[0], Integer.parseInt(parts[1]));
          } catch (NumberFormatException ignored) {
            // Ignore non-integers since our tests only use integer parameters
          }
        }
      }
    }
    return IndicatorDefinition.builder()
        .indicatorType(type)
        .source(source)
        .params(paramMap)
        .build();
  }

  private DailyIndicator dailyPoint(
      IndicatorType type,
      PriceSource source,
      String params,
      LocalDate date,
      Map<String, Double> values) {
    Map<String, BigDecimal> valMap = new java.util.HashMap<>();
    values.forEach((k, v) -> valMap.put(k, BigDecimal.valueOf(v)));
    return DailyIndicator.builder()
        .ticker(TICKER)
        .indicatorDefinition(definition(type, source, params))
        .priceDate(date)
        .values(valMap)
        .build();
  }

  private WeeklyIndicator weeklyPoint(
      IndicatorType type,
      PriceSource source,
      String params,
      LocalDate date,
      Map<String, Double> values) {
    Map<String, BigDecimal> valMap = new java.util.HashMap<>();
    values.forEach((k, v) -> valMap.put(k, BigDecimal.valueOf(v)));
    return WeeklyIndicator.builder()
        .ticker(TICKER)
        .indicatorDefinition(definition(type, source, params))
        .priceDate(date)
        .values(valMap)
        .build();
  }

  @Test
  void testWeeklyMacdEvaluatorPositiveCrossover() {
    WeeklyMacdEvaluator evaluator = new WeeklyMacdEvaluator();

    List<WeeklyIndicator> points =
        List.of(
            weeklyPoint(
                IndicatorType.MACD,
                PriceSource.CLOSE,
                "fast=12,slow=26,signal=9",
                YESTERDAY,
                Map.of("macd", 1.0, "signal", 1.2)),
            weeklyPoint(
                IndicatorType.MACD,
                PriceSource.CLOSE,
                "fast=12,slow=26,signal=9",
                TODAY,
                Map.of("macd", 1.5, "signal", 1.3)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(List.of(), List.of(), points);

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.tradeAction());
    assertEquals(EvaluatorMessage.POSITIVE_CROSSOVER.getValue(), signal.reason());
  }

  @Test
  void testDailyStochasticEvaluatorKGreaterThanD() {
    DailyStochasticEvaluator evaluator = new DailyStochasticEvaluator();

    List<DailyIndicator> points =
        List.of(
            dailyPoint(
                IndicatorType.STOCHASTIC,
                PriceSource.CLOSE,
                "k=14,kSmooth=3,dSmooth=3",
                YESTERDAY,
                Map.of("k", 71.0, "d", 70.0)),
            dailyPoint(
                IndicatorType.STOCHASTIC,
                PriceSource.CLOSE,
                "k=14,kSmooth=3,dSmooth=3",
                TODAY,
                Map.of("k", 82.0, "d", 80.0)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(List.of(), points, List.of());

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.tradeAction());
    assertEquals(EvaluatorMessage.K_ABOVE_D.getValue(), signal.reason());
  }

  @Test
  void testDailyRsiEvaluatorUptick() {
    DailyRsiEvaluator evaluator = new DailyRsiEvaluator();

    List<DailyIndicator> points =
        List.of(
            dailyPoint(
                IndicatorType.RSI,
                PriceSource.CLOSE,
                "period=14",
                YESTERDAY,
                Map.of("value", 45.0)),
            dailyPoint(
                IndicatorType.RSI, PriceSource.CLOSE, "period=14", TODAY, Map.of("value", 50.0)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(List.of(), points, List.of());

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.tradeAction());
    assertEquals(EvaluatorMessage.UPTICK.getValue(), signal.reason());
  }

  @Test
  void testDailyVolumeEvaluatorHeavyVolumeGreen() {
    DailyVolumeEvaluator evaluator = new DailyVolumeEvaluator();

    List<DailyPrice> candles =
        List.of(
            candle(YESTERDAY, 100, 101, 1000), candle(TODAY, 102, 105, 1500) // Green, heavy volume
            );

    List<DailyIndicator> volSmaSeries =
        List.of(
            dailyPoint(
                IndicatorType.SMA,
                PriceSource.VOLUME,
                "period=20",
                TODAY,
                Map.of("value", 1100.0)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(candles, volSmaSeries, List.of());

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.tradeAction());
    assertEquals(EvaluatorMessage.GREEN_CANDLE_HEAVY_VOLUME.getValue(), signal.reason());
  }

  @Test
  void testDailyEmaEvaluatorStrongBuy() {
    DailyEmaEvaluator evaluator = new DailyEmaEvaluator();

    LocalDate d3 = TODAY.minusDays(3);
    LocalDate d2 = TODAY.minusDays(2);

    List<DailyIndicator> points =
        List.of(
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=5", d3, Map.of("value", 101.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=5", d2, Map.of("value", 102.0)),
            dailyPoint(
                IndicatorType.EMA,
                PriceSource.CLOSE,
                "period=5",
                YESTERDAY,
                Map.of("value", 103.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=5", TODAY, Map.of("value", 105.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=13", d3, Map.of("value", 100.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=13", d2, Map.of("value", 101.0)),
            dailyPoint(
                IndicatorType.EMA,
                PriceSource.CLOSE,
                "period=13",
                YESTERDAY,
                Map.of("value", 102.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=13", TODAY, Map.of("value", 103.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=26", d3, Map.of("value", 99.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=26", d2, Map.of("value", 100.0)),
            dailyPoint(
                IndicatorType.EMA,
                PriceSource.CLOSE,
                "period=26",
                YESTERDAY,
                Map.of("value", 101.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=26", TODAY, Map.of("value", 102.0)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(List.of(), points, List.of());

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.STRONG_BUY, signal.tradeAction());
    assertEquals(EvaluatorMessage.EMA_STRONG_BUY.getValue(), signal.reason());
  }

  @Test
  void testDailyEmaEvaluatorCrossoverBuy() {
    DailyEmaEvaluator evaluator = new DailyEmaEvaluator();

    List<DailyIndicator> points =
        List.of(
            dailyPoint(
                IndicatorType.EMA,
                PriceSource.CLOSE,
                "period=5",
                YESTERDAY,
                Map.of("value", 100.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=5", TODAY, Map.of("value", 103.0)),
            dailyPoint(
                IndicatorType.EMA,
                PriceSource.CLOSE,
                "period=13",
                YESTERDAY,
                Map.of("value", 101.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=13", TODAY, Map.of("value", 101.5)),
            dailyPoint(
                IndicatorType.EMA,
                PriceSource.CLOSE,
                "period=26",
                YESTERDAY,
                Map.of("value", 102.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=26", TODAY, Map.of("value", 102.5)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(List.of(), points, List.of());

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.tradeAction());
    assertEquals(EvaluatorMessage.EMA_5_POS_CROSS_13_26.getValue(), signal.reason());
  }

  @Test
  void testDailyEmaEvaluatorStrongSell() {
    DailyEmaEvaluator evaluator = new DailyEmaEvaluator();

    List<DailyIndicator> points =
        List.of(
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=5", TODAY, Map.of("value", 95.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=13", TODAY, Map.of("value", 98.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=26", TODAY, Map.of("value", 100.0)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(List.of(), points, List.of());

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.STRONG_SELL, signal.tradeAction());
    assertEquals(EvaluatorMessage.EMA_STRONG_SELL.getValue(), signal.reason());
  }

  @Test
  void testDailyEmaEvaluatorSell() {
    DailyEmaEvaluator evaluator = new DailyEmaEvaluator();

    List<DailyIndicator> points =
        List.of(
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=5", TODAY, Map.of("value", 95.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=13", TODAY, Map.of("value", 100.0)),
            dailyPoint(
                IndicatorType.EMA, PriceSource.CLOSE, "period=26", TODAY, Map.of("value", 98.0)));
    ASTAEvaluationContext context = new ASTAEvaluationContext(List.of(), points, List.of());

    TradeSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.SELL, signal.tradeAction());
    assertEquals(EvaluatorMessage.EMA_5_NEG_CROSS_13_26.getValue(), signal.reason());
  }
}
