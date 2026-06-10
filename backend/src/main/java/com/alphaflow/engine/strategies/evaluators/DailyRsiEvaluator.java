package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DailyRsiEvaluator implements ASTAEvaluator {

  @Override
  public CalculatedSignal evaluate(ASTAEvaluationContext context) {
    Optional<IndicatorSeriesDTO> rsiSeriesOpt =
        context.dailyIndicators().stream()
            .filter(s -> "RSI".equalsIgnoreCase(s.type()))
            .findFirst();

    if (rsiSeriesOpt.isEmpty() || rsiSeriesOpt.get().points().size() < 2) {
      return new CalculatedSignal(TradeAction.HOLD, "Insufficient RSI data");
    }

    List<IndicatorPointDTO> points = rsiSeriesOpt.get().points();
    IndicatorPointDTO latest = points.getLast();
    IndicatorPointDTO prev = points.get(points.size() - 2);

    BigDecimal rsi0 = latest.getValue(IndicatorOutputKey.VALUE);
    BigDecimal rsi1 = prev.getValue(IndicatorOutputKey.VALUE);

    if (rsi0 == null || rsi1 == null) {
      return new CalculatedSignal(TradeAction.HOLD, "Missing RSI values");
    }

    if (rsi0.compareTo(rsi1) > 0) {
      return new CalculatedSignal(
          TradeAction.BUY, "Uptick (RSI: " + rsi0.setScale(1, RoundingMode.HALF_UP) + ")");
    } else if (rsi0.compareTo(rsi1) < 0) {
      return new CalculatedSignal(
          TradeAction.SELL, "Downtick (RSI: " + rsi0.setScale(1, RoundingMode.HALF_UP) + ")");
    } else {
      return new CalculatedSignal(
          TradeAction.HOLD, "Flat (RSI: " + rsi0.setScale(1, RoundingMode.HALF_UP) + ")");
    }
  }
}
