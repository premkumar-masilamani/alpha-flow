package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StateCodecTest {

  @Test
  void testConstructor() throws Exception {
    Constructor<StateCodec> constructor = StateCodec.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    StateCodec instance = constructor.newInstance();
    assertNotNull(instance);
  }

  @Test
  void testRoundTrip() {
    Map<String, BigDecimal> state = Map.of("ema", new BigDecimal("1.234567890123"));
    String json = StateCodec.encode(state);
    Map<String, BigDecimal> decoded = StateCodec.decode(json);
    assertEquals(state, decoded);
  }

  @Test
  void testDecodeEmpty() {
    assertTrue(StateCodec.decode(null).isEmpty());
    assertTrue(StateCodec.decode("").isEmpty());
    assertTrue(StateCodec.decode("   ").isEmpty());
  }

  @Test
  void testDecodeInvalidJson() {
    assertThrows(IllegalStateException.class, () -> StateCodec.decode("{invalid"));
  }

  @Test
  void testEncodeException() throws Exception {
    ObjectMapper originalMapper = getMapperField();
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    when(mockMapper.writeValueAsString(anyMap()))
        .thenThrow(new JsonProcessingException("mocked exception") {});

    try {
      setMapperField(mockMapper);
      assertThrows(
          IllegalStateException.class, () -> StateCodec.encode(Map.of("ema", BigDecimal.ONE)));
    } finally {
      setMapperField(originalMapper);
    }
  }

  private ObjectMapper getMapperField() throws Exception {
    Field field = StateCodec.class.getDeclaredField("MAPPER");
    field.setAccessible(true);
    return (ObjectMapper) field.get(null);
  }

  private void setMapperField(ObjectMapper value) throws Exception {
    Field field = StateCodec.class.getDeclaredField("MAPPER");
    field.setAccessible(true);
    Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    theUnsafe.setAccessible(true);
    sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
    Object base = unsafe.staticFieldBase(field);
    long offset = unsafe.staticFieldOffset(field);
    unsafe.putObject(base, offset, value);
  }
}
