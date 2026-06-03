package com.alphaflow.engine.configs;


import com.alphaflow.engine.calculators.indicators.IndicatorParams;

import com.alphaflow.engine.configs.IndicatorProperties.IndicatorDefinition;

import com.alphaflow.persistence.enums.IndicatorType;

import com.alphaflow.persistence.enums.PriceSource;

import com.alphaflow.persistence.enums.Timeframe;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;

import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;


import java.util.HashMap;

import java.util.List;

import java.util.Map;


import static org.junit.jupiter.api.Assertions.assertEquals;


/**

 * Verifies the v1 indicator matrix binds via {@code @ConfigurationProperties} exactly as written —

 * in particular that mixed-case param map keys ({@code kSmooth}, {@code dSmooth}) survive binding,

 * which is the subtle gotcha when binding {@code Map<String, ?>} keys.

 */

class IndicatorPropertiesBindingTest {


  private static IndicatorProperties bind() {

    Map<String, String> props = new HashMap<>();

    props.put("alphaflow.indicators.timeframes.daily[0].type", "EMA");

    props.put("alphaflow.indicators.timeframes.daily[0].params.period", "5");

    props.put("alphaflow.indicators.timeframes.daily[1].type", "EMA");

    props.put("alphaflow.indicators.timeframes.daily[1].params.period", "13");

    props.put("alphaflow.indicators.timeframes.daily[2].type", "EMA");

    props.put("alphaflow.indicators.timeframes.daily[2].params.period", "26");

    props.put("alphaflow.indicators.timeframes.daily[3].type", "RSI");

    props.put("alphaflow.indicators.timeframes.daily[3].params.period", "14");

    props.put("alphaflow.indicators.timeframes.daily[4].type", "STOCHASTIC");

    props.put("alphaflow.indicators.timeframes.daily[4].params.k", "14");

    props.put("alphaflow.indicators.timeframes.daily[4].params.kSmooth", "3");

    props.put("alphaflow.indicators.timeframes.daily[4].params.dSmooth", "3");

    props.put("alphaflow.indicators.timeframes.daily[5].type", "SMA");

    props.put("alphaflow.indicators.timeframes.daily[5].source", "VOLUME");

    props.put("alphaflow.indicators.timeframes.daily[5].params.period", "20");

    props.put("alphaflow.indicators.timeframes.weekly[0].type", "MACD");

    props.put("alphaflow.indicators.timeframes.weekly[0].params.fast", "12");

    props.put("alphaflow.indicators.timeframes.weekly[0].params.slow", "26");

    props.put("alphaflow.indicators.timeframes.weekly[0].params.signal", "9");


    return new Binder(new MapConfigurationPropertySource(props))

        .bind("alphaflow.indicators", IndicatorProperties.class)

        .get();

  }


  @Test

  void bindsDailyAndWeeklyMatrix() {

    IndicatorProperties properties = bind();


    List<IndicatorDefinition> daily = properties.forTimeframe(Timeframe.DAILY);

    assertEquals(6, daily.size());

    assertEquals(IndicatorType.EMA, daily.get(0).getType());

    assertEquals(PriceSource.CLOSE, daily.get(0).getSource(), "source defaults to CLOSE");

    assertEquals(5, daily.get(0).getParams().get("period"));


    IndicatorDefinition smaVolume = daily.get(5);

    assertEquals(IndicatorType.SMA, smaVolume.getType());

    assertEquals(PriceSource.VOLUME, smaVolume.getSource());


    List<IndicatorDefinition> weekly = properties.forTimeframe(Timeframe.WEEKLY);

    assertEquals(1, weekly.size());

    assertEquals(IndicatorType.MACD, weekly.get(0).getType());

  }


  @Test

  void preservesMixedCaseParamKeys() {

    IndicatorDefinition stochastic = bind().forTimeframe(Timeframe.DAILY).get(4);

    assertEquals(IndicatorType.STOCHASTIC, stochastic.getType());

    // The exact keys the StochasticIndicator reads must survive binding.

    assertEquals(14, stochastic.getParams().get("k"));

    assertEquals(3, stochastic.getParams().get("kSmooth"));

    assertEquals(3, stochastic.getParams().get("dSmooth"));

  }


  @Test

  void canonicalParamsAreStableRegardlessOfOrder() {

    IndicatorDefinition macd = bind().forTimeframe(Timeframe.WEEKLY).get(0);

    // Sorted canonical form is deterministic, so it is safe as part of the storage natural key.

    assertEquals("fast=12,signal=9,slow=26", IndicatorParams.of(macd.getParams()).canonical());

  }

}

