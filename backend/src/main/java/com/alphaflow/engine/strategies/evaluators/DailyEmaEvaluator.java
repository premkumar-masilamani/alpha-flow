package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.enums.EvaluatorMessage;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorParamKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailyEmaEvaluator implements ASTAEvaluator {

  @Override
  public TradeSignal evaluate(ASTAEvaluationContext context) {
    List<DailyIndicator> ema5Points =
        context.dailyIndicators().stream()
            .filter(
                s ->
                    s.getIndicatorType() == IndicatorType.EMA
                        && s.getParams().contains(IndicatorParamKey.PERIOD.getValue() + "=5"))
            .toList();
    List<DailyIndicator> ema13Points =
        context.dailyIndicators().stream()
            .filter(
                s ->
                    s.getIndicatorType() == IndicatorType.EMA
                        && s.getParams().contains(IndicatorParamKey.PERIOD.getValue() + "=13"))
            .toList();
    List<DailyIndicator> ema26Points =
        context.dailyIndicators().stream()
            .filter(
                s ->
                    s.getIndicatorType() == IndicatorType.EMA
                        && s.getParams().contains(IndicatorParamKey.PERIOD.getValue() + "=26"))
            .toList();

    if (ema5Points.isEmpty() || ema13Points.isEmpty() || ema26Points.isEmpty()) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.INSUFFICIENT_EMA_DATA.getValue());
    }

    BigDecimal e5_0 = ema5Points.getLast().getValues().get(IndicatorOutputKey.VALUE.getValue());
    BigDecimal e13_0 = ema13Points.getLast().getValues().get(IndicatorOutputKey.VALUE.getValue());
    BigDecimal e26_0 = ema26Points.getLast().getValues().get(IndicatorOutputKey.VALUE.getValue());

    if (e5_0 == null || e13_0 == null || e26_0 == null) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.MISSING_EMA_VALUES.getValue());
    }

    // Check Strong Buy first
    if (e5_0.compareTo(e13_0) > 0 && e13_0.compareTo(e26_0) > 0) {
      return new TradeSignal(TradeAction.STRONG_BUY, EvaluatorMessage.EMA_STRONG_BUY.getValue());
    }

    // Check Buy: 5 > 13 and 5 > 26 (but not Strong Buy)
    if (e5_0.compareTo(e13_0) > 0 && e5_0.compareTo(e26_0) > 0) {
      return new TradeSignal(TradeAction.BUY, EvaluatorMessage.EMA_5_POS_CROSS_13_26.getValue());
    }

    // Check Strong Sell
    if (e5_0.compareTo(e13_0) < 0 && e13_0.compareTo(e26_0) < 0) {
      return new TradeSignal(TradeAction.STRONG_SELL, EvaluatorMessage.EMA_STRONG_SELL.getValue());
    }

    // Check Sell: 5 < 13 and 5 < 26 (but not Strong Sell)
    if (e5_0.compareTo(e13_0) < 0 && e5_0.compareTo(e26_0) < 0) {
      return new TradeSignal(TradeAction.SELL, EvaluatorMessage.EMA_5_NEG_CROSS_13_26.getValue());
    }

    return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.MIXED_EMAS.getValue());
  }
}
