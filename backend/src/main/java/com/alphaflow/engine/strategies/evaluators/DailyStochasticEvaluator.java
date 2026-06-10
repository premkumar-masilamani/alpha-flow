package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.enums.EvaluatorMessage;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailyStochasticEvaluator implements ASTAEvaluator {

  @Override
  public TradeSignal evaluate(ASTAEvaluationContext context) {
    List<DailyIndicator> stochPoints =
        context.dailyIndicators().stream()
            .filter(s -> s.getIndicatorType() == IndicatorType.STOCHASTIC)
            .toList();

    if (stochPoints.size() < 2) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.INSUFFICIENT_STOCH_DATA.getValue());
    }

    DailyIndicator latest = stochPoints.getLast();
    DailyIndicator prev = stochPoints.get(stochPoints.size() - 2);

    BigDecimal k0 = latest.getValues().get(IndicatorOutputKey.K.getValue());
    BigDecimal d0 = latest.getValues().get(IndicatorOutputKey.D.getValue());
    BigDecimal k1 = prev.getValues().get(IndicatorOutputKey.K.getValue());
    BigDecimal d1 = prev.getValues().get(IndicatorOutputKey.D.getValue());

    if (k0 == null || d0 == null || k1 == null || d1 == null) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.MISSING_STOCH_VALUES.getValue());
    }

    boolean crossoverBuy = k0.compareTo(d0) > 0 && k1.compareTo(d1) <= 0;
    boolean crossoverSell = k0.compareTo(d0) < 0 && k1.compareTo(d1) >= 0;

    if (crossoverBuy) {
      return new TradeSignal(TradeAction.BUY, EvaluatorMessage.POSITIVE_CROSSOVER.getValue());
    } else if (crossoverSell) {
      return new TradeSignal(TradeAction.SELL, EvaluatorMessage.NEGATIVE_CROSSOVER.getValue());
    } else if (k0.compareTo(d0) > 0) {
      return new TradeSignal(TradeAction.BUY, EvaluatorMessage.K_ABOVE_D.getValue());
    } else if (k0.compareTo(d0) < 0) {
      return new TradeSignal(TradeAction.SELL, EvaluatorMessage.K_BELOW_D.getValue());
    } else {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.K_EQUAL_D.getValue());
    }
  }
}
