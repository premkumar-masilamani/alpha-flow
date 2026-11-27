package com.alphaflow.fixtures;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public record MarketDataFixture(
    String resourcePath,
    List<PriceBar> bars,
    Map<String, Map<LocalDate, BigDecimal>> expectedColumns) {

  public MarketDataFixture {
    if (StringUtils.isNotBlank(resourcePath)) {
      if (bars != null && expectedColumns != null) {
        bars = List.copyOf(bars);
        Map<String, Map<LocalDate, BigDecimal>> copiedColumns = new LinkedHashMap<>();
        for (Map.Entry<String, Map<LocalDate, BigDecimal>> entry : expectedColumns.entrySet()) {
          String columnName = entry.getKey();
          Map<LocalDate, BigDecimal> series = entry.getValue();
          if (StringUtils.isNotBlank(columnName) && series != null) {
            copiedColumns.put(columnName, Collections.unmodifiableMap(new LinkedHashMap<>(series)));
          } else {
            throw new IllegalArgumentException(
                "Fixture expected column name must be non-blank and series must be non-null: "
                    + resourcePath);
          }
        }
        expectedColumns = Collections.unmodifiableMap(copiedColumns);
      } else {
        throw new IllegalArgumentException(
            "Fixture bars and expectedColumns must be non-null: " + resourcePath);
      }
    } else {
      throw new IllegalArgumentException("Fixture resourcePath must be non-blank");
    }
  }

  public Map<LocalDate, BigDecimal> expectedSeries(String columnName) {
    Map<LocalDate, BigDecimal> series = expectedColumns.get(columnName);
    if (series != null) {
      return series;
    } else {
      throw new IllegalArgumentException(
          "Unknown fixture column '"
              + columnName
              + "' in "
              + resourcePath
              + ". Available columns: "
              + expectedColumns.keySet());
    }
  }

  public Set<String> columnNames() {
    return Collections.unmodifiableSet(expectedColumns.keySet());
  }
}
