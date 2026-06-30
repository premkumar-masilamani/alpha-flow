package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void resolvesCachedDefinitionsBasedOnConfiguredIds() {
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

    // Manually simulate properties binding
    indicatorConfig.getTimeframes().put(Timeframe.DAILY, List.of(101L));
    indicatorConfig.getTimeframes().put(Timeframe.WEEKLY, List.of(202L, 999L)); // 999 is missing

    // Execute init
    indicatorConfig.init();

    List<IndicatorDefinition> daily = indicatorConfig.forTimeframe(Timeframe.DAILY);
    assertEquals(1, daily.size());
    assertEquals(IndicatorType.EMA, daily.getFirst().getType());
    assertEquals(101L, daily.getFirst().getIndicatorId());

    List<IndicatorDefinition> weekly = indicatorConfig.forTimeframe(Timeframe.WEEKLY);
    assertEquals(1, weekly.size()); // 999 was skipped
    assertEquals(IndicatorType.MACD, weekly.getFirst().getType());
    assertEquals(202L, weekly.getFirst().getIndicatorId());
  }
}
