package com.alphaflow.api.configs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.format.FormatterRegistry;

class WebMvcConfigTest {

  @Test
  void testAddFormattersRegistersStringToEnumConverterFactory() {
    WebMvcConfig config = new WebMvcConfig();
    FormatterRegistry registry = mock(FormatterRegistry.class);

    config.addFormatters(registry);

    verify(registry).addConverterFactory(any(StringToEnumConverterFactory.class));
  }
}
