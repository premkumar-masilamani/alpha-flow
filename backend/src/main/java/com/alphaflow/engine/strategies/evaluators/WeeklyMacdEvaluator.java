package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.enums.EvaluatorMessage;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WeeklyMacdEvaluator implements ASTAEvaluator {

  @Override
  public TradeSignal evaluate(ASTAEvaluationContext context) {
    List<WeeklyIndicator> macdPoints =
        context.weeklyIndicators().stream()
            .filter(s -> s.getIndicatorType() == IndicatorType.MACD)
            .toList();

    if (macdPoints.size() < 2) {
      return new TradeSignal(
          TradeAction.HOLD, EvaluatorMessage.INSUFFICIENT_WEEKLY_MACD_DATA.getValue());
    }

    WeeklyIndicator latest = macdPoints.getLast();
    WeeklyIndicator prev = macdPoints.get(macdPoints.size() - 2);

    BigDecimal macd0 = latest.getValues().get(IndicatorOutputKey.MACD.getValue());
    BigDecimal sig0 = latest.getValues().get(IndicatorOutputKey.SIGNAL.getValue());
    BigDecimal macd1 = prev.getValues().get(IndicatorOutputKey.MACD.getValue());
    BigDecimal sig1 = prev.getValues().get(IndicatorOutputKey.SIGNAL.getValue());

    if (macd0 == null || sig0 == null || macd1 == null || sig1 == null) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.MISSING_MACD_VALUES.getValue());
    }

    boolean crossoverBuy = macd0.compareTo(sig0) > 0 && macd1.compareTo(sig1) <= 0;
    boolean crossoverSell = macd0.compareTo(sig0) < 0 && macd1.compareTo(sig1) >= 0;

    if (crossoverBuy) {
      return new TradeSignal(TradeAction.BUY, EvaluatorMessage.POSITIVE_CROSSOVER.getValue());
    } else if (crossoverSell) {
      return new TradeSignal(TradeAction.SELL, EvaluatorMessage.NEGATIVE_CROSSOVER.getValue());
    } else if (macd0.compareTo(sig0) > 0) {
      BigDecimal hist0 = macd0.subtract(sig0);
      BigDecimal hist1 = macd1.subtract(sig1);
      EvaluatorMessage message =
          hist0.compareTo(hist1) > 0
              ? EvaluatorMessage.UPTICK
              : EvaluatorMessage.FLAT_AFTER_DOWN_RARE;
      return new TradeSignal(TradeAction.BUY, message.getValue());
    } else if (macd0.compareTo(sig0) < 0) {
      BigDecimal hist0 = macd0.subtract(sig0);
      BigDecimal hist1 = macd1.subtract(sig1);
      EvaluatorMessage message =
          hist0.compareTo(hist1) < 0
              ? EvaluatorMessage.DOWNTICK
              : EvaluatorMessage.FLAT_AFTER_UP_RARE;
      return new TradeSignal(TradeAction.SELL, message.getValue());
    } else {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.MACD_EQUAL_SIGNAL.getValue());
    }
  }
}
