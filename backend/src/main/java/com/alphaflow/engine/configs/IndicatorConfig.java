package com.alphaflow.engine.configs;

import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds the in-memory cache of technical indicator configurations. Loads them from the database at
 * application startup and maps them to configured timeframes from application properties.
 */
@Component
@ConfigurationProperties(prefix = "alphaflow.indicators")
public class IndicatorConfig {

  private static final Logger log = LoggerFactory.getLogger(IndicatorConfig.class);

  private final IndicatorDefinitionRepository repository;

  /** Timeframe mappings configured in properties (e.g. daily=1,2,3). */
  private final Map<Timeframe, List<Long>> timeframes = new EnumMap<>(Timeframe.class);

  /** Resolved actual indicator definitions, cached by timeframe. */
  private final Map<Timeframe, List<IndicatorDefinition>> cachedDefinitions =
      new EnumMap<>(Timeframe.class);

  /**
   * Constructs an IndicatorConfig.
   *
   * @param repository the repository for loading indicator definitions
   */
  public IndicatorConfig(IndicatorDefinitionRepository repository) {
    this.repository = repository;
  }

  /**
   * Returns the bound timeframe mappings from properties.
   *
   * @return timeframe to indicator ID mapping
   */
  public Map<Timeframe, List<Long>> getTimeframes() {
    return timeframes;
  }

  /** Loads all indicator definitions and maps them according to configuration properties. */
  @PostConstruct
  public void init() {
    loadFromDatabase();
  }

  /** Fetches definitions, indexes by ID, and groups them in-memory by timeframe. */
  public synchronized void loadFromDatabase() {
    log.info("Loading indicator definitions from database...");
    List<IndicatorDefinition> all = repository.findAll();

    Map<Long, IndicatorDefinition> definitionMap = new HashMap<>();
    for (IndicatorDefinition def : all) {
      definitionMap.put(def.getIndicatorId(), def);
    }

    Map<Timeframe, List<IndicatorDefinition>> tempMap = new EnumMap<>(Timeframe.class);
    for (Timeframe tf : Timeframe.values()) {
      tempMap.put(tf, new ArrayList<>());
      List<Long> ids = timeframes.getOrDefault(tf, List.of());
      for (Long id : ids) {
        IndicatorDefinition def = definitionMap.get(id);
        if (def != null) {
          tempMap.get(tf).add(def);
        } else {
          log.warn(
              "Configured indicator ID {} for timeframe {} was not found in the database"
                  + " definitions!",
              id,
              tf);
        }
      }
    }

    cachedDefinitions.clear();
    cachedDefinitions.putAll(tempMap);
    log.info("Successfully loaded and mapped indicator definitions from database.");
  }

  /**
   * Returns configured indicators for a given timeframe.
   *
   * @param timeframe the timeframe
   * @return list of active indicator definitions
   */
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
