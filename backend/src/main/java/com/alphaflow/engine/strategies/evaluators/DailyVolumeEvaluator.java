package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.enums.EvaluatorMessage;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailyVolumeEvaluator implements ASTAEvaluator {

  @Override
  public TradeSignal evaluate(ASTAEvaluationContext context) {
    if (context.dailyCandles().size() < 2) {
      return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.INSUFFICIENT_PRICE_DATA.getValue());
    }

    DailyPrice latestCandle = context.dailyCandles().getLast();

    List<DailyIndicator> volSmaPoints =
        context.dailyIndicators().stream()
            .filter(
                s ->
                    s.getIndicatorType() == IndicatorType.SMA
                        && s.getSource() == PriceSource.VOLUME)
            .toList();

    if (volSmaPoints.isEmpty()) {
      return new TradeSignal(
          TradeAction.HOLD, EvaluatorMessage.INSUFFICIENT_VOLUME_SMA_DATA.getValue());
    }

    BigDecimal volSmaValue =
        volSmaPoints.getLast().getValues().get(IndicatorOutputKey.VALUE.getValue());

    if (volSmaValue == null) {
      return new TradeSignal(
          TradeAction.HOLD, EvaluatorMessage.MISSING_VOLUME_SMA_VALUE.getValue());
    }

    BigDecimal volume = latestCandle.getVolume();
    boolean isHeavyVolume = volume.compareTo(volSmaValue) > 0;
    boolean isGreen = latestCandle.getPriceClose().compareTo(latestCandle.getPriceOpen()) > 0;
    boolean isRed = latestCandle.getPriceClose().compareTo(latestCandle.getPriceOpen()) < 0;

    if (isHeavyVolume) {
      if (isGreen) {
        return new TradeSignal(
            TradeAction.BUY, EvaluatorMessage.GREEN_CANDLE_HEAVY_VOLUME.getValue());
      } else if (isRed) {
        return new TradeSignal(
            TradeAction.SELL, EvaluatorMessage.RED_CANDLE_HEAVY_VOLUME.getValue());
      } else {
        return new TradeSignal(TradeAction.HOLD, EvaluatorMessage.DOJI_HEAVY_VOLUME.getValue());
      }
    } else {
      EvaluatorMessage message =
          isGreen
              ? EvaluatorMessage.GREEN_CANDLE_NORMAL_VOLUME
              : (isRed
                  ? EvaluatorMessage.RED_CANDLE_NORMAL_VOLUME
                  : EvaluatorMessage.DOJI_NORMAL_VOLUME);
      return new TradeSignal(TradeAction.HOLD, message.getValue());
    }
  }
}
