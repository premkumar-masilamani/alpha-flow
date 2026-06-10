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
public class DailyStochasticEvaluator implements ASTAEvaluator {

  @Override
  public CalculatedSignal evaluate(ASTAEvaluationContext context) {
    Optional<IndicatorSeriesDTO> stochSeriesOpt =
        context.dailyIndicators().stream()
            .filter(s -> "STOCHASTIC".equalsIgnoreCase(s.type()))
            .findFirst();

    if (stochSeriesOpt.isEmpty() || stochSeriesOpt.get().points().size() < 2) {
      return new CalculatedSignal(TradeAction.HOLD, "Insufficient stochastic data");
    }

    List<IndicatorPointDTO> points = stochSeriesOpt.get().points();
    IndicatorPointDTO latest = points.getLast();
    IndicatorPointDTO prev = points.get(points.size() - 2);

    BigDecimal k0 = latest.getValue(IndicatorOutputKey.K);
    BigDecimal d0 = latest.getValue(IndicatorOutputKey.D);
    BigDecimal k1 = prev.getValue(IndicatorOutputKey.K);
    BigDecimal d1 = prev.getValue(IndicatorOutputKey.D);

    if (k0 == null || d0 == null || k1 == null || d1 == null) {
      return new CalculatedSignal(TradeAction.HOLD, "Missing Stoch K/D values");
    }

    boolean crossoverBuy = k0.compareTo(d0) > 0 && k1.compareTo(d1) <= 0;
    boolean crossoverSell = k0.compareTo(d0) < 0 && k1.compareTo(d1) >= 0;

    if (crossoverBuy) {
      return new CalculatedSignal(TradeAction.BUY, "Positive Crossover");
    } else if (crossoverSell) {
      return new CalculatedSignal(TradeAction.SELL, "Negative Crossover");
    } else if (k0.compareTo(d0) > 0) {
      return new CalculatedSignal(TradeAction.BUY, "K > D");
    } else if (k0.compareTo(d0) < 0) {
      return new CalculatedSignal(TradeAction.SELL, "K < D");
    } else {
      return new CalculatedSignal(TradeAction.HOLD, "K = D");
    }
  }
}
