package com.alphaflow.engine.configs;

import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import jakarta.annotation.PostConstruct;
import java.util.*;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds the in-memory cache of technical indicator configurations. Loads them from the database at
 * application startup and maps them to configured timeframes from application properties.
 */
@Component
@ConfigurationProperties(prefix = "alphaflow.indicators")
public class IndicatorConfig {

  private static final Logger logger = LoggerFactory.getLogger(IndicatorConfig.class);

  private final IndicatorDefinitionRepository indicatorDefinitionRepository;
  @Getter private final Map<Timeframe, List<Long>> timeframes = new EnumMap<>(Timeframe.class);
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

  public synchronized void loadFromDatabase() {
    logger.info("Loading indicator definitions from database...");
    List<IndicatorDefinition> all = indicatorDefinitionRepository.findAll();

    Map<Long, IndicatorDefinition> definitionMap = new HashMap<>();
    for (IndicatorDefinition def : all) {
      definitionMap.put(def.getIndicatorId(), def);
    }

    Map<Timeframe, List<IndicatorDefinition>> mapFromConfig = new EnumMap<>(Timeframe.class);
    for (Timeframe timeframe : Timeframe.values()) {
      mapFromConfig.put(timeframe, new ArrayList<>());
      List<Long> timeframeIDs = timeframes.getOrDefault(timeframe, List.of());
      for (Long timeframeID : timeframeIDs) {
        IndicatorDefinition indicatorDefinition = definitionMap.get(timeframeID);
        if (indicatorDefinition != null) {
          mapFromConfig.get(timeframe).add(indicatorDefinition);
        } else {
          logger.warn(
              "Configured indicator ID {} for timeframe {} was not found in the database"
                  + " definitions!",
              timeframeID,
              timeframe);
        }
      }
    }

    cachedDefinitions.clear();
    cachedDefinitions.putAll(mapFromConfig);
    logger.info("Successfully loaded and mapped indicator definitions from database.");
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
