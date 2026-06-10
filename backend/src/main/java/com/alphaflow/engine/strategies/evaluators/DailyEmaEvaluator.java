package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DailyEmaEvaluator implements ASTAEvaluator {

  @Override
  public CalculatedSignal evaluate(ASTAEvaluationContext context) {
    Optional<IndicatorSeriesDTO> ema5Opt =
        context.dailyIndicators().stream()
            .filter(s -> "EMA".equalsIgnoreCase(s.type()) && s.params().contains("period=5"))
            .findFirst();
    Optional<IndicatorSeriesDTO> ema13Opt =
        context.dailyIndicators().stream()
            .filter(s -> "EMA".equalsIgnoreCase(s.type()) && s.params().contains("period=13"))
            .findFirst();
    Optional<IndicatorSeriesDTO> ema26Opt =
        context.dailyIndicators().stream()
            .filter(s -> "EMA".equalsIgnoreCase(s.type()) && s.params().contains("period=26"))
            .findFirst();

    if (ema5Opt.isEmpty()
        || ema13Opt.isEmpty()
        || ema26Opt.isEmpty()
        || ema5Opt.get().points().size() < 2
        || ema13Opt.get().points().size() < 2
        || ema26Opt.get().points().size() < 2) {
      return new CalculatedSignal(TradeAction.HOLD, "Insufficient EMA data");
    }

    List<IndicatorPointDTO> points5 = ema5Opt.get().points();
    List<IndicatorPointDTO> points13 = ema13Opt.get().points();
    List<IndicatorPointDTO> points26 = ema26Opt.get().points();

    BigDecimal e5_0 = points5.getLast().getValue(IndicatorOutputKey.VALUE);
    BigDecimal e5_1 = points5.get(points5.size() - 2).getValue(IndicatorOutputKey.VALUE);

    BigDecimal e13_0 = points13.getLast().getValue(IndicatorOutputKey.VALUE);
    BigDecimal e13_1 = points13.get(points13.size() - 2).getValue(IndicatorOutputKey.VALUE);

    BigDecimal e26_0 = points26.getLast().getValue(IndicatorOutputKey.VALUE);
    BigDecimal e26_1 = points26.get(points26.size() - 2).getValue(IndicatorOutputKey.VALUE);

    if (e5_0 == null
        || e5_1 == null
        || e13_0 == null
        || e13_1 == null
        || e26_0 == null
        || e26_1 == null) {
      return new CalculatedSignal(TradeAction.HOLD, "Missing EMA values");
    }

    boolean pco13 = e5_0.compareTo(e13_0) > 0 && e5_1.compareTo(e13_1) <= 0;
    boolean pco26 = e5_0.compareTo(e26_0) > 0 && e5_1.compareTo(e26_1) <= 0;
    boolean nco13 = e5_0.compareTo(e13_0) < 0 && e5_1.compareTo(e13_1) >= 0;
    boolean nco26 = e5_0.compareTo(e26_0) < 0 && e5_1.compareTo(e26_1) >= 0;

    if (pco13 || pco26) {
      String value =
          pco13 && pco26
              ? "5 EMA Positive Crossover with 13 & 26 EMA"
              : (pco13
                  ? "5 EMA Positive Crossover with 13 EMA"
                  : "5 EMA Positive Crossover with 26 EMA");
      return new CalculatedSignal(TradeAction.BUY, value);
    } else if (nco13 || nco26) {
      String value =
          nco13 && nco26
              ? "5 EMA Negative Crossover with 13 & 26 EMA"
              : (nco13
                  ? "5 EMA Negative Crossover with 13 EMA"
                  : "5 EMA Negative Crossover with 26 EMA");
      return new CalculatedSignal(TradeAction.SELL, value);
    } else if (e5_0.compareTo(e13_0) > 0 && e5_0.compareTo(e26_0) > 0) {
      return new CalculatedSignal(TradeAction.BUY, "EMA 5 > 13 & 26 (Bullish Alignment)");
    } else if (e5_0.compareTo(e13_0) < 0 && e5_0.compareTo(e26_0) < 0) {
      return new CalculatedSignal(TradeAction.SELL, "EMA 5 < 13 & 26 (Bearish Alignment)");
    } else {
      return new CalculatedSignal(TradeAction.HOLD, "Mixed EMAs");
    }
  }
}
