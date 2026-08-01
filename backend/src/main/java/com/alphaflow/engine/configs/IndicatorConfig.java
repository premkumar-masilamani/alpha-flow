package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Holds the in-memory cache of technical indicator configurations. Loads them from the database at
 * application startup and maps them to all supported timeframes.
 */
@Component
public class IndicatorConfig {

  private static final Logger logger = LoggerFactory.getLogger(IndicatorConfig.class);

  private final IndicatorDefinitionRepository indicatorDefinitionRepository;
  private final Map<Timeframe, List<IndicatorDefinition>> cachedDefinitions =
      new EnumMap<>(Timeframe.class);

  @Autowired
  public IndicatorConfig(IndicatorDefinitionRepository indicatorDefinitionRepository) {
    this.indicatorDefinitionRepository = indicatorDefinitionRepository;
  }

  @PostConstruct
  public void init() {
    loadFromDatabase();
  }

  /**
   * Loads all indicator definitions from the database and maps them to all timeframes.
   */
  public synchronized void loadFromDatabase() {
    logger.info("Loading indicator definitions from database...");
    List<IndicatorDefinition> all = indicatorDefinitionRepository.findAll();

    cachedDefinitions.clear();
    for (Timeframe timeframe : Timeframe.values()) {
      cachedDefinitions.put(timeframe, new ArrayList<>(all));
    }
    logger.info(
        "Successfully loaded and mapped {} indicator definitions for all timeframes.",
        all.size());
  }

  public List<IndicatorDefinition> forTimeframe(Timeframe timeframe) {
    return cachedDefinitions.getOrDefault(timeframe, List.of());
  }

  /**
   * Directly sets the cached definitions. Intended primarily for testing.
   *
   * @param definitions the pre-configured definitions map
   */
  public synchronized void setCachedDefinitions(
      Map<Timeframe, List<IndicatorDefinition>> definitions) {
    this.cachedDefinitions.clear();
    this.cachedDefinitions.putAll(definitions);
  }
}
