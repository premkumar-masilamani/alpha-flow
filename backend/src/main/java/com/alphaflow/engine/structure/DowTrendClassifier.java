package com.alphaflow.engine.structure;

import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.DowTrend;
import com.alphaflow.engine.structure.enums.SwingType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Classifies the prevailing Dow Theory trend immediately preceding a structural pattern using the
 * four confirmed alternating swing pivots prior to the pattern's starting pivot.
 */
@Component
public class DowTrendClassifier {

  /** Number of preceding alternating swings required to compare two highs and two lows. */
  public static final int PRIOR_SWING_WINDOW = 4;

  /**
   * Classifies the prior Dow Theory trend leading into the pivot at {@code patternStartIndex}.
   *
   * @param swings strictly alternating confirmed swing pivots
   * @param patternStartIndex index of the first pivot of the candidate chart pattern
   * @return {@link DowTrend#UPTREND} for HH+HL, {@link DowTrend#DOWNTREND} for LH+LL, or {@link
   *     DowTrend#SIDEWAYS} otherwise
   */
  public DowTrend classifyPriorTrend(List<SwingPoint> swings, int patternStartIndex) {
    if (swings != null
        && patternStartIndex >= PRIOR_SWING_WINDOW
        && patternStartIndex <= swings.size()) {
      SwingPoint firstPrior = swings.get(patternStartIndex - 4);
      SwingPoint secondPrior = swings.get(patternStartIndex - 3);
      SwingPoint thirdPrior = swings.get(patternStartIndex - 2);
      SwingPoint fourthPrior = swings.get(patternStartIndex - 1);

      BigDecimal earlierHigh;
      BigDecimal laterHigh;
      BigDecimal earlierLow;
      BigDecimal laterLow;

      if (firstPrior.type() == SwingType.HIGH) {
        earlierHigh = firstPrior.price();
        earlierLow = secondPrior.price();
        laterHigh = thirdPrior.price();
        laterLow = fourthPrior.price();
      } else {
        earlierLow = firstPrior.price();
        earlierHigh = secondPrior.price();
        laterLow = thirdPrior.price();
        laterHigh = fourthPrior.price();
      }

      boolean higherHigh = laterHigh.compareTo(earlierHigh) > 0;
      boolean higherLow = laterLow.compareTo(earlierLow) > 0;
      boolean lowerHigh = laterHigh.compareTo(earlierHigh) < 0;
      boolean lowerLow = laterLow.compareTo(earlierLow) < 0;

      if (higherHigh && higherLow) {
        return DowTrend.UPTREND;
      } else if (lowerHigh && lowerLow) {
        return DowTrend.DOWNTREND;
      } else {
        return DowTrend.SIDEWAYS;
      }
    } else {
      return DowTrend.SIDEWAYS;
    }
  }
}
