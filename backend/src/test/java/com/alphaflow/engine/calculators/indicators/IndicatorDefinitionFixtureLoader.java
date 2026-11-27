package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.engine.indicators.BollingerBandsIndicator;
import com.alphaflow.engine.indicators.EmaIndicator;
import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.engine.indicators.MacdIndicator;
import com.alphaflow.engine.indicators.RsiIndicator;
import com.alphaflow.engine.indicators.SmaIndicator;
import com.alphaflow.engine.indicators.StochasticIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.utils.IndicatorRegistry;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public final class IndicatorDefinitionFixtureLoader {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final IndicatorRegistry INDICATOR_REGISTRY =
      new IndicatorRegistry(
          List.of(
              new SmaIndicator(),
              new EmaIndicator(),
              new RsiIndicator(),
              new MacdIndicator(),
              new StochasticIndicator(),
              new BollingerBandsIndicator()));

  private static final String FIELD_NAME = "name";
  private static final String FIELD_INDICATOR_TYPE = "indicatorType";
  private static final String FIELD_SOURCE = "source";
  private static final String FIELD_PARAMS = "params";
  private static final String FIELD_OUTPUT_KEY_TO_COLUMN = "outputKeyToColumn";

  public static List<IndicatorFixtureCase> load(String classpathResource) {
    if (StringUtils.isNotBlank(classpathResource)) {
      String normalizedPath =
          classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
      try (InputStream resourceStream =
          IndicatorDefinitionFixtureLoader.class.getResourceAsStream(normalizedPath)) {
        if (resourceStream != null) {
          JsonNode rootNode = OBJECT_MAPPER.readTree(resourceStream);
          return parseDefinitions(classpathResource, rootNode);
        } else {
          throw new IllegalStateException(
              "Indicator definitions fixture not found on classpath: " + classpathResource);
        }
      } catch (IOException ioException) {
        throw new IllegalStateException(
            "Failed to read indicator definitions fixture: " + classpathResource, ioException);
      }
    } else {
      throw new IllegalArgumentException(
          "Indicator definitions fixture classpathResource must be non-blank");
    }
  }

  private static List<IndicatorFixtureCase> parseDefinitions(String sourceName, JsonNode rootNode) {
    if (rootNode != null && rootNode.isArray() && !rootNode.isEmpty()) {
      List<IndicatorFixtureCase> definitions = new ArrayList<>();
      for (int i = 0; i < rootNode.size(); i++) {
        JsonNode entryNode = rootNode.get(i);
        definitions.add(parseEntry(sourceName, i, entryNode));
      }
      return List.copyOf(definitions);
    } else {
      throw new IllegalArgumentException(
          "Indicator definitions fixture must be a non-empty JSON array: " + sourceName);
    }
  }

  private static IndicatorFixtureCase parseEntry(String sourceName, int index, JsonNode entryNode) {
    String name = requireText(sourceName, index, entryNode, FIELD_NAME);
    String indicatorTypeText = requireText(sourceName, index, entryNode, FIELD_INDICATOR_TYPE);
    String sourceText = requireText(sourceName, index, entryNode, FIELD_SOURCE);
    String paramsText = requireText(sourceName, index, entryNode, FIELD_PARAMS);

    IndicatorType indicatorType = IndicatorType.from(indicatorTypeText);
    PriceSource priceSource = PriceSource.from(sourceText);
    Indicator indicator = INDICATOR_REGISTRY.get(indicatorType);
    IndicatorParams params = IndicatorParams.parse(paramsText);

    Map<String, String> outputKeyToColumn = parseOutputMapping(sourceName, index, entryNode);
    return new IndicatorFixtureCase(name, indicator, params, priceSource, outputKeyToColumn);
  }

  private static String requireText(
      String sourceName, int index, JsonNode entryNode, String fieldName) {
    JsonNode fieldNode = entryNode.get(fieldName);
    if (fieldNode != null && fieldNode.isTextual() && StringUtils.isNotBlank(fieldNode.asText())) {
      return fieldNode.asText().trim();
    } else {
      throw new IllegalArgumentException(
          "Missing or blank field '" + fieldName + "' at index " + index + " in " + sourceName);
    }
  }

  private static Map<String, String> parseOutputMapping(
      String sourceName, int index, JsonNode entryNode) {
    JsonNode mappingNode = entryNode.get(FIELD_OUTPUT_KEY_TO_COLUMN);
    if (mappingNode != null && mappingNode.isObject() && !mappingNode.isEmpty()) {
      Map<String, String> outputKeyToColumn = new LinkedHashMap<>();
      for (Map.Entry<String, JsonNode> field : mappingNode.properties()) {
        String outputKey = field.getKey();
        JsonNode columnNode = field.getValue();
        if (StringUtils.isNotBlank(outputKey)
            && columnNode != null
            && columnNode.isTextual()
            && StringUtils.isNotBlank(columnNode.asText())) {
          outputKeyToColumn.put(outputKey.trim(), columnNode.asText().trim());
        } else {
          throw new IllegalArgumentException(
              "Invalid outputKeyToColumn entry at index " + index + " in " + sourceName);
        }
      }
      return outputKeyToColumn;
    } else {
      throw new IllegalArgumentException(
          "Missing or empty '"
              + FIELD_OUTPUT_KEY_TO_COLUMN
              + "' object at index "
              + index
              + " in "
              + sourceName);
    }
  }
}
