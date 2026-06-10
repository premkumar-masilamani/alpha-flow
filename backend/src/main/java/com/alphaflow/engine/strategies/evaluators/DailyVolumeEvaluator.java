package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.TradeAction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DailyVolumeEvaluator implements ASTAEvaluator {

  @Override
  public CalculatedSignal evaluate(ASTAEvaluationContext context) {
    if (context.dailyCandles().size() < 2) {
      return new CalculatedSignal(TradeAction.HOLD, "Insufficient price data");
    }

    OhlcvDTO latestCandle = context.dailyCandles().getLast();

    Optional<IndicatorSeriesDTO> volSmaOpt =
        context.dailyIndicators().stream()
            .filter(s -> "SMA".equalsIgnoreCase(s.type()) && "VOLUME".equalsIgnoreCase(s.source()))
            .findFirst();

    if (volSmaOpt.isEmpty() || volSmaOpt.get().points().isEmpty()) {
      return new CalculatedSignal(TradeAction.HOLD, "Insufficient volume SMA data");
    }

    List<IndicatorPointDTO> smaPoints = volSmaOpt.get().points();
    IndicatorPointDTO latestSma = smaPoints.getLast();
    BigDecimal volSmaValue = latestSma.getValue(IndicatorOutputKey.VALUE);

    if (volSmaValue == null) {
      return new CalculatedSignal(TradeAction.HOLD, "Missing volume SMA value");
    }

    BigDecimal volume = latestCandle.volume();
    boolean isHeavyVolume = volume.compareTo(volSmaValue) > 0;
    boolean isGreen = latestCandle.priceClose().compareTo(latestCandle.priceOpen()) > 0;
    boolean isRed = latestCandle.priceClose().compareTo(latestCandle.priceOpen()) < 0;

    if (isHeavyVolume) {
      if (isGreen) {
        return new CalculatedSignal(TradeAction.BUY, "Green Candle with Heavy Volume");
      } else if (isRed) {
        return new CalculatedSignal(TradeAction.SELL, "Red Candle with Heavy Volume");
      } else {
        return new CalculatedSignal(TradeAction.HOLD, "Doji with Heavy Volume");
      }
    } else {
      String val =
          isGreen
              ? "Green Candle with Normal Volume"
              : (isRed ? "Red Candle with Normal Volume" : "Doji with Normal Volume");
      return new CalculatedSignal(TradeAction.HOLD, val);
    }
  }
}
