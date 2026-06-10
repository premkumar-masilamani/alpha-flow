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
public class WeeklyMacdEvaluator implements ASTAEvaluator {

  @Override
  public CalculatedSignal evaluate(ASTAEvaluationContext context) {
    Optional<IndicatorSeriesDTO> macdSeriesOpt =
        context.weeklyIndicators().stream()
            .filter(s -> "MACD".equalsIgnoreCase(s.type()))
            .findFirst();

    if (macdSeriesOpt.isEmpty() || macdSeriesOpt.get().points().size() < 2) {
      return new CalculatedSignal(TradeAction.HOLD, "Insufficient weekly MACD data");
    }

    List<IndicatorPointDTO> points = macdSeriesOpt.get().points();
    IndicatorPointDTO latest = points.getLast();
    IndicatorPointDTO prev = points.get(points.size() - 2);

    BigDecimal macd0 = latest.getValue(IndicatorOutputKey.MACD);
    BigDecimal sig0 = latest.getValue(IndicatorOutputKey.SIGNAL);
    BigDecimal macd1 = prev.getValue(IndicatorOutputKey.MACD);
    BigDecimal sig1 = prev.getValue(IndicatorOutputKey.SIGNAL);

    if (macd0 == null || sig0 == null || macd1 == null || sig1 == null) {
      return new CalculatedSignal(TradeAction.HOLD, "Missing MACD/Signal values");
    }

    boolean crossoverBuy = macd0.compareTo(sig0) > 0 && macd1.compareTo(sig1) <= 0;
    boolean crossoverSell = macd0.compareTo(sig0) < 0 && macd1.compareTo(sig1) >= 0;

    if (crossoverBuy) {
      return new CalculatedSignal(TradeAction.BUY, "Positive Crossover");
    } else if (crossoverSell) {
      return new CalculatedSignal(TradeAction.SELL, "Negative Crossover");
    } else if (macd0.compareTo(sig0) > 0) {
      BigDecimal hist0 = macd0.subtract(sig0);
      BigDecimal hist1 = macd1.subtract(sig1);
      String value = hist0.compareTo(hist1) > 0 ? "Uptick" : "Flat after down (rare)";
      return new CalculatedSignal(TradeAction.BUY, value);
    } else if (macd0.compareTo(sig0) < 0) {
      BigDecimal hist0 = macd0.subtract(sig0);
      BigDecimal hist1 = macd1.subtract(sig1);
      String value = hist0.compareTo(hist1) < 0 ? "Downtick" : "Flat after up (rare)";
      return new CalculatedSignal(TradeAction.SELL, value);
    } else {
      return new CalculatedSignal(TradeAction.HOLD, "MACD = Signal");
    }
  }
}
