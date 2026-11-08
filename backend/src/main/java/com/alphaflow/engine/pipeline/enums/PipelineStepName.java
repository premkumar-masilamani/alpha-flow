package com.alphaflow.engine.pipeline.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * Enum representing all independent execution steps in Alpha Flow's Spring Batch pipelines.
 */
public enum PipelineStepName {
  DOWNLOAD_DAILY_YAHOO_FINANCE,
  DOWNLOAD_INTRADAY_ANGEL_ONE,
  DOWNLOAD_INTRADAY_ANGEL_ONE_LIVE_QUOTE,

  CALCULATE_DAILY_PRICE_FROM_INTRADAY,

  CALCULATE_DAILY_SUPPORT_RESISTANCE,
  CALCULATE_DAILY_CANDLESTICK_PATTERN,
  CALCULATE_DAILY_CHART_PATTERN,
  CALCULATE_DAILY_INDICATOR,
  CALCULATE_WEEKLY_PRICE,
  CALCULATE_WEEKLY_SUPPORT_RESISTANCE,
  CALCULATE_WEEKLY_CANDLESTICK_PATTERN,
  CALCULATE_WEEKLY_CHART_PATTERN,
  CALCULATE_WEEKLY_INDICATOR,

  CALCULATE_SUPPORT_RESISTANCE,
  CALCULATE_CANDLESTICK_PATTERN,
  CALCULATE_CHART_PATTERN,
  CALCULATE_INDICATOR,

  STRATEGY_INTRADAY_70P_BOUNCE;

  /**
   * Resolves a {@link PipelineStepName} from a string representation (case-insensitive).
   *
   * @param text raw step name string
   * @return matching {@link PipelineStepName}
   * @throws IllegalArgumentException if the text is blank or does not match any step
   */
  public static PipelineStepName from(String text) {
    if (StringUtils.isNotBlank(text)) {
      String normalized = text.trim().toUpperCase();
      for (PipelineStepName stepName : values()) {
        if (stepName.name().equals(normalized)) {
          return stepName;
        }
      }
      throw new IllegalArgumentException("Unknown pipeline step name: " + text);
    } else {
      throw new IllegalArgumentException("Pipeline step name must not be blank");
    }
  }
}
