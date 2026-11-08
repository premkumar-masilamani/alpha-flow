package com.alphaflow.engine.pipeline.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * Enum representing all independent execution steps in Alpha Flow's Spring Batch pipelines.
 */
public enum PipelineStepName {
  YAHOO_DAILY_DOWNLOAD,
  DAILY_SUPPORT_RESISTANCE,
  DAILY_CANDLESTICK_PATTERN,
  DAILY_CHART_PATTERN,
  DAILY_INDICATOR,
  WEEKLY_PRICE,
  WEEKLY_SUPPORT_RESISTANCE,
  WEEKLY_CANDLESTICK_PATTERN,
  WEEKLY_CHART_PATTERN,
  WEEKLY_INDICATOR,
  ANGEL_ONE_INTRADAY_DOWNLOAD,
  ANGEL_ONE_LIVE_QUOTE_DOWNLOAD,
  INTRADAY_70P_BOUNCE_STRATEGY;

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
