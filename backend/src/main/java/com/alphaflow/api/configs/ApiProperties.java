package com.alphaflow.api.configs;


import com.alphaflow.persistence.enums.Timeframe;

import java.util.EnumMap;

import java.util.Map;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.context.annotation.Configuration;


/**

 * API read tunables. {@code window} caps how many of the most recent bars the OHLCV and indicator

 * endpoints return, per timeframe, so chart overlays line up with the candles they sit on. Bound

 * from {@code alphaflow.api.window}; falls back to {@link #DEFAULT_WINDOW} when unset.

 */

@Configuration

@ConfigurationProperties(prefix = "alphaflow.api")

@Data

public class ApiProperties {


  private static final int DEFAULT_WINDOW = 180;


  private int window = DEFAULT_WINDOW;


  public int windowFor(Timeframe timeframe) {

    return window;

  }

}

