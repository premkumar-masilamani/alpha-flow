package com.alphaflow.engine.strategies.evaluators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvaluatorsTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);
  private static final LocalDate YESTERDAY = LocalDate.of(2026, 5, 29);

  private OhlcvDTO candle(LocalDate date, double open, double close, double volume) {
    return OhlcvDTO.builder()
        .priceDate(date)
        .priceOpen(BigDecimal.valueOf(open))
        .priceClose(BigDecimal.valueOf(close))
        .volume(BigDecimal.valueOf(volume))
        .build();
  }

  private IndicatorSeriesDTO series(
      String type, String source, String params, List<IndicatorPointDTO> points) {
    return IndicatorSeriesDTO.builder()
        .type(type)
        .source(source)
        .params(params)
        .points(points)
        .build();
  }

  private IndicatorPointDTO point(LocalDate date, Map<String, BigDecimal> values) {
    return IndicatorPointDTO.builder().date(date).values(values).build();
  }

  @Test
  void testWeeklyMacdEvaluatorPositiveCrossover() {
    WeeklyMacdEvaluator evaluator = new WeeklyMacdEvaluator();

    List<IndicatorPointDTO> points =
        List.of(
            point(
                YESTERDAY,
                Map.of("macd", BigDecimal.valueOf(1.0), "signal", BigDecimal.valueOf(1.2))),
            point(
                TODAY, Map.of("macd", BigDecimal.valueOf(1.5), "signal", BigDecimal.valueOf(1.3))));
    IndicatorSeriesDTO macdSeries = series("MACD", "CLOSE", "fast=12,slow=26,signal=9", points);
    ASTAEvaluationContext context =
        new ASTAEvaluationContext(List.of(), List.of(), List.of(macdSeries));

    CalculatedSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.signal());
    assertEquals("Positive Crossover", signal.value());
  }

  @Test
  void testDailyStochasticEvaluatorKGreaterThanD() {
    DailyStochasticEvaluator evaluator = new DailyStochasticEvaluator();

    List<IndicatorPointDTO> points =
        List.of(
            point(YESTERDAY, Map.of("k", BigDecimal.valueOf(71), "d", BigDecimal.valueOf(70))),
            point(TODAY, Map.of("k", BigDecimal.valueOf(82), "d", BigDecimal.valueOf(80))));
    IndicatorSeriesDTO stochSeries =
        series("STOCHASTIC", "CLOSE", "k=14,kSmooth=3,dSmooth=3", points);
    ASTAEvaluationContext context =
        new ASTAEvaluationContext(List.of(), List.of(stochSeries), List.of());

    CalculatedSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.signal());
    assertEquals("K > D", signal.value());
  }

  @Test
  void testDailyRsiEvaluatorUptick() {
    DailyRsiEvaluator evaluator = new DailyRsiEvaluator();

    List<IndicatorPointDTO> points =
        List.of(
            point(YESTERDAY, Map.of("value", BigDecimal.valueOf(45.0))),
            point(TODAY, Map.of("value", BigDecimal.valueOf(50.0))));
    IndicatorSeriesDTO rsiSeries = series("RSI", "CLOSE", "period=14", points);
    ASTAEvaluationContext context =
        new ASTAEvaluationContext(List.of(), List.of(rsiSeries), List.of());

    CalculatedSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.signal());
    assertEquals("Uptick (RSI: 50.0)", signal.value());
  }

  @Test
  void testDailyVolumeEvaluatorHeavyVolumeGreen() {
    DailyVolumeEvaluator evaluator = new DailyVolumeEvaluator();

    List<OhlcvDTO> candles =
        List.of(
            candle(YESTERDAY, 100, 101, 1000), candle(TODAY, 102, 105, 1500) // Green, heavy volume
            );

    List<IndicatorPointDTO> smaPoints =
        List.of(
            point(TODAY, Map.of("value", BigDecimal.valueOf(1100.0))) // Volume SMA
            );
    IndicatorSeriesDTO volSmaSeries = series("SMA", "VOLUME", "period=20", smaPoints);
    ASTAEvaluationContext context =
        new ASTAEvaluationContext(candles, List.of(volSmaSeries), List.of());

    CalculatedSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.signal());
    assertEquals("Green Candle with Heavy Volume", signal.value());
  }

  @Test
  void testDailyEmaEvaluatorBullishAlignment() {
    DailyEmaEvaluator evaluator = new DailyEmaEvaluator();

    IndicatorSeriesDTO ema5 =
        series(
            "EMA",
            "CLOSE",
            "period=5",
            List.of(
                point(YESTERDAY, Map.of("value", BigDecimal.valueOf(101.0))),
                point(TODAY, Map.of("value", BigDecimal.valueOf(104.0)))));
    IndicatorSeriesDTO ema13 =
        series(
            "EMA",
            "CLOSE",
            "period=13",
            List.of(
                point(YESTERDAY, Map.of("value", BigDecimal.valueOf(100.0))),
                point(TODAY, Map.of("value", BigDecimal.valueOf(102.0)))));
    IndicatorSeriesDTO ema26 =
        series(
            "EMA",
            "CLOSE",
            "period=26",
            List.of(
                point(YESTERDAY, Map.of("value", BigDecimal.valueOf(99.0))),
                point(TODAY, Map.of("value", BigDecimal.valueOf(101.0)))));

    ASTAEvaluationContext context =
        new ASTAEvaluationContext(List.of(), List.of(ema5, ema13, ema26), List.of());

    CalculatedSignal signal = evaluator.evaluate(context);
    assertEquals(TradeAction.BUY, signal.signal());
    assertEquals("EMA 5 > 13 & 26 (Bullish Alignment)", signal.value());
  }
}
