package com.alphaflow.api.dtos;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;

public class TemporalAccessorSerializer extends JsonSerializer<TemporalAccessor> {

  @Override
  public void serialize(
      TemporalAccessor value, JsonGenerator generator, SerializerProvider serializers)
      throws IOException {
    if (value == null) {
      generator.writeNull();
    } else if (value instanceof LocalDate localDate) {
      generator.writeString(localDate.toString());
    } else if (value instanceof OffsetDateTime offsetDateTime) {
      generator.writeString(offsetDateTime.toString());
    } else {
      generator.writeString(value.toString());
    }
  }
}
