package com.alphaflow.engine.calculators.indicators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * (De)serializes recursive indicators' running state to/from the {@code jsonb} blob stored in
 * {@code indicator_state.internals}.
 * <p>
 * Decimals are stored as <b>strings</b> (e.g. {@code {"ema":"123.456789012345"}}), never JSON
 * floats, so the running state never round-trips through {@code double} and resume stays bit-exact.
 */
public final class StateCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StateCodec() {
    }

    public static String encode(Map<String, BigDecimal> state) {
        Map<String, String> asStrings = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : state.entrySet()) {
            asStrings.put(e.getKey(), e.getValue().toPlainString());
        }
        try {
            return MAPPER.writeValueAsString(asStrings);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to encode indicator state: " + asStrings, ex);
        }
    }

    public static Map<String, BigDecimal> decode(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> raw = MAPPER.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
            Map<String, BigDecimal> decoded = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : raw.entrySet()) {
                decoded.put(e.getKey(), new BigDecimal(e.getValue()));
            }
            return decoded;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to decode indicator state: " + json, ex);
        }
    }
}
