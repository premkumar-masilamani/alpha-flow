package com.alphaflow.api.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TemporalAccessorSerializerTest {

  @Test
  void testSerialize() throws Exception {
    TemporalAccessorSerializer serializer = new TemporalAccessorSerializer();
    ObjectMapper objectMapper = new ObjectMapper();

    // null
    StringWriter nullWriter = new StringWriter();
    JsonGenerator nullGenerator = new JsonFactory().createGenerator(nullWriter);
    serializer.serialize(null, nullGenerator, objectMapper.getSerializerProvider());
    nullGenerator.flush();
    assertEquals("null", nullWriter.toString());

    // LocalDate
    StringWriter dateWriter = new StringWriter();
    JsonGenerator dateGenerator = new JsonFactory().createGenerator(dateWriter);
    serializer.serialize(
        LocalDate.of(2026, 10, 2), dateGenerator, objectMapper.getSerializerProvider());
    dateGenerator.flush();
    assertEquals("\"2026-10-02\"", dateWriter.toString());

    // OffsetDateTime
    OffsetDateTime offsetDateTime = OffsetDateTime.of(2026, 10, 2, 9, 15, 0, 0, ZoneOffset.UTC);
    StringWriter offsetDateTimeWriter = new StringWriter();
    JsonGenerator offsetDateTimeGenerator = new JsonFactory().createGenerator(offsetDateTimeWriter);
    serializer.serialize(
        offsetDateTime, offsetDateTimeGenerator, objectMapper.getSerializerProvider());
    offsetDateTimeGenerator.flush();
    assertEquals("\"2026-10-02T09:15Z\"", offsetDateTimeWriter.toString());

    // LocalDateTime
    LocalDateTime localDateTime = LocalDateTime.of(2026, 10, 2, 9, 15, 0);
    StringWriter localDateTimeWriter = new StringWriter();
    JsonGenerator localDateTimeGenerator = new JsonFactory().createGenerator(localDateTimeWriter);
    serializer.serialize(
        localDateTime, localDateTimeGenerator, objectMapper.getSerializerProvider());
    localDateTimeGenerator.flush();
    assertEquals("\"2026-10-02T09:15\"", localDateTimeWriter.toString());
  }
}
