package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies that IndicatorConfig binds timeframes and resolves cached definitions. */
class IndicatorConfigBindingTest {

  private IndicatorDefinitionRepository indicatorDefinitionRepository;
  private IndicatorConfig indicatorConfig;

  @BeforeEach
  void setUp() {
    indicatorDefinitionRepository = mock(IndicatorDefinitionRepository.class);
    indicatorConfig = new IndicatorConfig(indicatorDefinitionRepository);
  }

  @Test
  void mapsAllDefinitionsToSupportedTimeframes() {
    IndicatorDefinition ema =
        IndicatorDefinition.builder()
            .indicatorId(101L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 5))
            .build();

    IndicatorDefinition macd =
        IndicatorDefinition.builder()
            .indicatorId(202L)
            .indicatorType(IndicatorType.MACD)
            .source(PriceSource.CLOSE)
            .params(Map.of("fast", 12, "slow", 26, "signal", 9))
            .build();

    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(ema, macd));

    // Execute init
    indicatorConfig.init();

    List<IndicatorDefinition> daily = indicatorConfig.forTimeframe(Timeframe.DAILY);
    assertEquals(2, daily.size());
    assertTrue(daily.contains(ema));
    assertTrue(daily.contains(macd));

    List<IndicatorDefinition> weekly = indicatorConfig.forTimeframe(Timeframe.WEEKLY);
    assertEquals(2, weekly.size());
    assertTrue(weekly.contains(ema));
    assertTrue(weekly.contains(macd));

    List<IndicatorDefinition> monthly = indicatorConfig.forTimeframe(Timeframe.MONTHLY);
    assertTrue(monthly.isEmpty());
  }
}
