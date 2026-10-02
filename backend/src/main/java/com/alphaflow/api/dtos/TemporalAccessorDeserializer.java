package com.alphaflow.api.dtos;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;

public class TemporalAccessorDeserializer extends JsonDeserializer<TemporalAccessor> {

  @Override
  public TemporalAccessor deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String text = parser.getText();
    if (text == null || text.isBlank()) {
      return null;
    }
    if (text.contains("T") || text.contains("+") || text.endsWith("Z")) {
      return OffsetDateTime.parse(text);
    }
    return LocalDate.parse(text);
  }
}
