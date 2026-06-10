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
public class DailyRsiEvaluator implements ASTAEvaluator {

  @Override
  public TradeSignal evaluate(ASTAEvaluationContext context) {
    List<DailyIndicator> rsiPoints =
        context.dailyIndicators().stream()
            .filter(s -> s.getIndicatorType() == IndicatorType.RSI)
            .toList();

    if (rsiPoints.size() < 2) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.INSUFFICIENT_RSI_DATA.getValue());
    }

    DailyIndicator latest = rsiPoints.getLast();
    DailyIndicator prev = rsiPoints.get(rsiPoints.size() - 2);

    BigDecimal rsi0 = latest.getValues().get(IndicatorOutputKey.VALUE.getValue());
    BigDecimal rsi1 = prev.getValues().get(IndicatorOutputKey.VALUE.getValue());

    if (rsi0 == null || rsi1 == null) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.MISSING_RSI_VALUES.getValue());
    }

    if (rsi0.compareTo(rsi1) > 0) {
      return new TradeSignal(TradeAction.BUY, EvaluatorMessage.UPTICK.getValue());
    } else if (rsi0.compareTo(rsi1) < 0) {
      return new TradeSignal(TradeAction.SELL, EvaluatorMessage.DOWNTICK.getValue());
    } else {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.FLAT.getValue());
    }
  }
}
