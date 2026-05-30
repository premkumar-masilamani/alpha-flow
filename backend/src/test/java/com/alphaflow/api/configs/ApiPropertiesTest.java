package com.alphaflow.api.configs;

import com.alphaflow.persistence.enums.Timeframe;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiPropertiesTest {

    @Test
    void testDefaultWindow() {
        ApiProperties props = new ApiProperties();
        assertEquals(180, props.windowFor(Timeframe.DAILY));
        assertEquals(180, props.windowFor(Timeframe.WEEKLY));
    }

    @Test
    void testConfiguredWindow() {
        ApiProperties props = new ApiProperties();
        Map<Timeframe, Integer> customWindow = new EnumMap<>(Timeframe.class);
        customWindow.put(Timeframe.DAILY, 100);
        props.setWindow(customWindow);

        assertEquals(100, props.windowFor(Timeframe.DAILY));
        assertEquals(180, props.windowFor(Timeframe.WEEKLY));
        assertEquals(customWindow, props.getWindow());
    }
}
