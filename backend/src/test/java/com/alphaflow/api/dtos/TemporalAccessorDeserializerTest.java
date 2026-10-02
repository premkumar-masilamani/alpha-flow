package com.alphaflow.api.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import org.junit.jupiter.api.Test;

class TemporalAccessorDeserializerTest {

  @Test
  void testDeserialize() throws Exception {
    TemporalAccessorDeserializer deserializer = new TemporalAccessorDeserializer();
    ObjectMapper objectMapper = new ObjectMapper();

    // null / empty
    JsonParser emptyParser = new JsonFactory().createParser("\"\"");
    emptyParser.nextToken();
    assertNull(deserializer.deserialize(emptyParser, objectMapper.getDeserializationContext()));

    // LocalDate string
    JsonParser dateParser = new JsonFactory().createParser("\"2026-10-02\"");
    dateParser.nextToken();
    TemporalAccessor dateResult =
        deserializer.deserialize(dateParser, objectMapper.getDeserializationContext());
    assertEquals(LocalDate.of(2026, 10, 2), dateResult);

    // OffsetDateTime string
    JsonParser odtParser = new JsonFactory().createParser("\"2026-10-02T09:15:00Z\"");
    odtParser.nextToken();
    TemporalAccessor odtResult =
        deserializer.deserialize(odtParser, objectMapper.getDeserializationContext());
    assertEquals(OffsetDateTime.parse("2026-10-02T09:15:00Z"), odtResult);
  }
}
