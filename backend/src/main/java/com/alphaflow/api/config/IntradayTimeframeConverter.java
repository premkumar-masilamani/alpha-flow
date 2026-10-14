package com.alphaflow.api.config;

import com.alphaflow.common.enums.IntradayTimeframe;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class IntradayTimeframeConverter implements Converter<String, IntradayTimeframe> {

  @Override
  public IntradayTimeframe convert(String source) {
    return IntradayTimeframe.from(source);
  }
}
