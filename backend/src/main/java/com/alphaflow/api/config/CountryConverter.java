package com.alphaflow.api.config;

import com.alphaflow.persistence.enums.Country;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CountryConverter implements Converter<String, Country> {

  @Override
  public Country convert(String source) {
    if (source != null && !source.trim().isEmpty()) {
      return Country.valueOf(source.trim().toUpperCase());
    } else {
      return null;
    }
  }
}
