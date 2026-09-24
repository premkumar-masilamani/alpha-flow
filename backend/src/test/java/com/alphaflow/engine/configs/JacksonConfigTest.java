package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JacksonConfigTest {

  @Test
  void testObjectMapperBean() {
    JacksonConfig config = new JacksonConfig();
    ObjectMapper mapper = config.objectMapper();
    assertNotNull(mapper);
    assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
  }
}
