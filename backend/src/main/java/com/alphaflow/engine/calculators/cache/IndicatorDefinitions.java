package com.alphaflow.engine.calculators.cache;

import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndicatorDefinitions {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndicatorDefinitions.class);

  private final IndicatorDefinitionRepository indicatorDefinitionRepository;
  private final List<IndicatorDefinition> cachedDefinitions = new ArrayList<>();

  @Autowired
  public IndicatorDefinitions(IndicatorDefinitionRepository indicatorDefinitionRepository) {
    this.indicatorDefinitionRepository = indicatorDefinitionRepository;
  }

  @PostConstruct
  public void init() {
    loadFromDatabase();
  }

  public synchronized void loadFromDatabase() {
    LOGGER.info("Loading indicator definitions from database...");
    List<IndicatorDefinition> all = indicatorDefinitionRepository.findAll();

    cachedDefinitions.clear();
    cachedDefinitions.addAll(all);
    LOGGER.info("Successfully loaded and cached {} indicator definitions.", all.size());
  }

  public List<IndicatorDefinition> getDefinitions() {
    return new ArrayList<>(cachedDefinitions);
  }

  public synchronized void setCachedDefinitions(List<IndicatorDefinition> definitions) {
    this.cachedDefinitions.clear();
    this.cachedDefinitions.addAll(definitions);
  }
}
