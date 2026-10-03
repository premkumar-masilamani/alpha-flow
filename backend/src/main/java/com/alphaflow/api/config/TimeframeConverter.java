package com.alphaflow.api.config;

import com.alphaflow.common.enums.Timeframe;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TimeframeConverter implements Converter<String, Timeframe> {

  @Override
  public Timeframe convert(String source) {
    return Timeframe.from(source);
  }
}
