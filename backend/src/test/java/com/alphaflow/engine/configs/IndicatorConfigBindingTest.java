package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.calculators.indicators.IndicatorParams;
import com.alphaflow.engine.configs.IndicatorConfig.IndicatorDefinition;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Verifies the v1 indicator matrix binds via {@code @ConfigurationProperties} exactly as written —
 *
 * <p>in particular that mixed-case param map keys ({@code kSmooth}, {@code dSmooth}) survive
 * binding,
 *
 * <p>which is the subtle gotcha when binding {@code Map<String, ?>} keys.
 */
class IndicatorConfigBindingTest {

  private static IndicatorConfig bind() {

    Map<String, String> props = new HashMap<>();

    props.put("alphaflow.indicators.timeframes.daily[0].type", "EMA");
    props.put("alphaflow.indicators.timeframes.daily[0].source", "CLOSE");
    props.put("alphaflow.indicators.timeframes.daily[0].params.period", "5");

    props.put("alphaflow.indicators.timeframes.daily[1].type", "EMA");
    props.put("alphaflow.indicators.timeframes.daily[1].source", "CLOSE");
    props.put("alphaflow.indicators.timeframes.daily[1].params.period", "13");

    props.put("alphaflow.indicators.timeframes.daily[2].type", "EMA");
    props.put("alphaflow.indicators.timeframes.daily[2].source", "CLOSE");
    props.put("alphaflow.indicators.timeframes.daily[2].params.period", "26");

    props.put("alphaflow.indicators.timeframes.daily[3].type", "RSI");
    props.put("alphaflow.indicators.timeframes.daily[3].source", "CLOSE");
    props.put("alphaflow.indicators.timeframes.daily[3].params.period", "14");

    props.put("alphaflow.indicators.timeframes.daily[4].type", "STOCHASTIC");
    props.put("alphaflow.indicators.timeframes.daily[4].source", "CLOSE");
    props.put("alphaflow.indicators.timeframes.daily[4].params.k", "14");
    props.put("alphaflow.indicators.timeframes.daily[4].params.kSmooth", "3");
    props.put("alphaflow.indicators.timeframes.daily[4].params.dSmooth", "3");

    props.put("alphaflow.indicators.timeframes.daily[5].type", "SMA");
    props.put("alphaflow.indicators.timeframes.daily[5].source", "VOLUME");
    props.put("alphaflow.indicators.timeframes.daily[5].params.period", "20");

    props.put("alphaflow.indicators.timeframes.weekly[0].type", "MACD");
    props.put("alphaflow.indicators.timeframes.weekly[0].source", "CLOSE");
    props.put("alphaflow.indicators.timeframes.weekly[0].params.fast", "12");
    props.put("alphaflow.indicators.timeframes.weekly[0].params.slow", "26");
    props.put("alphaflow.indicators.timeframes.weekly[0].params.signal", "9");

    return new Binder(new MapConfigurationPropertySource(props))
        .bind("alphaflow.indicators", IndicatorConfig.class)
        .get();
  }

  @Test
  void bindsDailyAndWeeklyMatrix() {

    IndicatorConfig properties = bind();

    List<IndicatorDefinition> daily = properties.forTimeframe(Timeframe.DAILY);

    assertEquals(6, daily.size());

    assertEquals(IndicatorType.EMA, daily.get(0).getType());

    assertEquals(PriceSource.CLOSE, daily.get(0).getSource(), "source is configured as CLOSE");

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

  @Test
  void validationFailsWhenSourceIsNull() {
    IndicatorConfig config = new IndicatorConfig();
    IndicatorDefinition def = new IndicatorDefinition();
    def.setType(IndicatorType.EMA);
    def.setSource(null); // Explicitly null
    def.getParams().put("period", 5);

    List<IndicatorDefinition> list = new ArrayList<>();
    list.add(def);
    config.getTimeframes().put(Timeframe.DAILY, list);

    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<IndicatorConfig>> violations = validator.validate(config);

      assertEquals(1, violations.size());
      ConstraintViolation<IndicatorConfig> violation = violations.iterator().next();
      assertTrue(violation.getPropertyPath().toString().contains("source"));
    }
  }
}
