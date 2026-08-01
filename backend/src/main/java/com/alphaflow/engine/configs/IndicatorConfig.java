package com.alphaflow.engine.configs;

import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Holds the in-memory cache of technical indicator configurations. Loads them from the database at application startup.
 */
@Component
public class IndicatorConfig {

  private static final Logger logger = LoggerFactory.getLogger(IndicatorConfig.class);

  private final IndicatorDefinitionRepository indicatorDefinitionRepository;
  private final List<IndicatorDefinition> cachedDefinitions = new ArrayList<>();

  @Autowired
  public IndicatorConfig(IndicatorDefinitionRepository indicatorDefinitionRepository) {
    this.indicatorDefinitionRepository = indicatorDefinitionRepository;
  }

  @PostConstruct
  public void init() {
    loadFromDatabase();
  }

  /** Loads all indicator definitions from the database and updates the in-memory cache. */
  public synchronized void loadFromDatabase() {
    logger.info("Loading indicator definitions from database...");
    List<IndicatorDefinition> all = indicatorDefinitionRepository.findAll();

    cachedDefinitions.clear();
    cachedDefinitions.addAll(all);
    logger.info("Successfully loaded and cached {} indicator definitions.", all.size());
  }

  public List<IndicatorDefinition> getDefinitions() {
    return new ArrayList<>(cachedDefinitions);
  }

  /**
   * Directly sets the cached definitions. Intended primarily for testing.
   *
   * @param definitions the pre-configured definitions list
   */
  public synchronized void setCachedDefinitions(List<IndicatorDefinition> definitions) {
    this.cachedDefinitions.clear();
    this.cachedDefinitions.addAll(definitions);
  }
}
