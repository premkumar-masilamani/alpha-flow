package com.alphaflow.api.config;

import com.alphaflow.common.enums.Timeframe;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TimeframeConverter implements Converter<String, Timeframe> {

  @Override
  public Timeframe convert(String source) {
    if (source == null || source.trim().isEmpty()) {
      return null;
    }
    String normalized = source.trim().toUpperCase();
    if ("15M".equals(normalized) || "15MIN".equals(normalized)) {
      return Timeframe.FIFTEEN_MINUTE;
    }
    return Timeframe.valueOf(normalized);
  }
}
