package com.alphaflow.fixtures;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public final class CsvFixtureLoader {

  private static final String COLUMN_PRICE_DATE = "price_date";
  private static final String COLUMN_PRICE_OPEN = "price_open";
  private static final String COLUMN_PRICE_HIGH = "price_high";
  private static final String COLUMN_PRICE_LOW = "price_low";
  private static final String COLUMN_PRICE_CLOSE = "price_close";
  private static final String COLUMN_VOLUME = "volume";
  private static final Set<String> REQUIRED_OHLCV_COLUMNS =
      Set.of(
          COLUMN_PRICE_DATE,
          COLUMN_PRICE_OPEN,
          COLUMN_PRICE_HIGH,
          COLUMN_PRICE_LOW,
          COLUMN_PRICE_CLOSE,
          COLUMN_VOLUME);

  public static MarketDataFixture load(String classpathResource) {
    if (StringUtils.isNotBlank(classpathResource)) {
      String normalizedPath =
          classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
      try (InputStream resourceStream =
          CsvFixtureLoader.class.getResourceAsStream(normalizedPath)) {
        if (resourceStream != null) {
          try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return load(classpathResource, reader);
          }
        } else {
          throw new IllegalStateException(
              "Fixture resource not found on classpath: " + classpathResource);
        }
      } catch (IOException ioException) {
        throw new IllegalStateException(
            "Failed to read fixture resource: " + classpathResource, ioException);
      }
    } else {
      throw new IllegalArgumentException("Fixture classpathResource must be non-blank");
    }
  }

  static MarketDataFixture load(String sourceName, BufferedReader reader) throws IOException {
    String headerLine = reader.readLine();
    if (StringUtils.isNotBlank(headerLine)) {
      String[] rawHeaders = headerLine.split(",", -1);
      Map<String, Integer> columnIndices = parseHeader(sourceName, rawHeaders);

      List<String> expectedColumnNames = new ArrayList<>();
      Map<String, Map<LocalDate, BigDecimal>> expectedColumns = new LinkedHashMap<>();
      for (String columnName : columnIndices.keySet()) {
        if (!REQUIRED_OHLCV_COLUMNS.contains(columnName)) {
          expectedColumnNames.add(columnName);
          expectedColumns.put(columnName, new LinkedHashMap<>());
        }
      }

      List<PriceBar> bars = new ArrayList<>();
      LocalDate previousDate = null;
      int lineNumber = 1;
      String line = reader.readLine();
      while (line != null) {
        lineNumber++;
        if (StringUtils.isNotBlank(line)) {
          String[] cells = line.split(",", -1);
          if (cells.length == rawHeaders.length) {
            LocalDate priceDate =
                parseDate(
                    sourceName,
                    lineNumber,
                    COLUMN_PRICE_DATE,
                    cells[columnIndices.get(COLUMN_PRICE_DATE)]);
            if (previousDate == null || priceDate.isAfter(previousDate)) {
              previousDate = priceDate;
              PriceBar bar = buildPriceBar(sourceName, lineNumber, priceDate, columnIndices, cells);
              bars.add(bar);
              recordExpectedColumns(
                  sourceName,
                  lineNumber,
                  priceDate,
                  expectedColumnNames,
                  columnIndices,
                  cells,
                  expectedColumns);
            } else {
              throw new IllegalArgumentException(
                  "Duplicate or non-chronological price_date '"
                      + priceDate
                      + "' after '"
                      + previousDate
                      + "' at "
                      + sourceName
                      + ":"
                      + lineNumber);
            }
          } else {
            throw new IllegalArgumentException(
                "Column count mismatch at "
                    + sourceName
                    + ":"
                    + lineNumber
                    + " (expected "
                    + rawHeaders.length
                    + ", found "
                    + cells.length
                    + ")");
          }
        }
        line = reader.readLine();
      }

      if (!bars.isEmpty()) {
        return new MarketDataFixture(sourceName, bars, expectedColumns);
      } else {
        throw new IllegalArgumentException("Fixture CSV contains no data rows: " + sourceName);
      }
    } else {
      throw new IllegalArgumentException("Fixture CSV is missing a header row: " + sourceName);
    }
  }

  private static Map<String, Integer> parseHeader(String sourceName, String... rawHeaders) {
    Map<String, Integer> columnIndices = new LinkedHashMap<>();
    for (int i = 0; i < rawHeaders.length; i++) {
      String columnName = rawHeaders[i].trim();
      if (StringUtils.isNotBlank(columnName)) {
        if (!columnIndices.containsKey(columnName)) {
          columnIndices.put(columnName, i);
        } else {
          throw new IllegalArgumentException(
              "Duplicate header column '" + columnName + "' in " + sourceName);
        }
      } else {
        throw new IllegalArgumentException(
            "Blank header column at index " + i + " in " + sourceName);
      }
    }

    if (columnIndices.keySet().containsAll(REQUIRED_OHLCV_COLUMNS)) {
      return columnIndices;
    } else {
      throw new IllegalArgumentException(
          "Fixture CSV is missing required OHLCV columns in "
              + sourceName
              + ". Required: "
              + REQUIRED_OHLCV_COLUMNS
              + ", Found: "
              + columnIndices.keySet());
    }
  }

  private static PriceBar buildPriceBar(
      String sourceName,
      int lineNumber,
      LocalDate priceDate,
      Map<String, Integer> columnIndices,
      String... cells) {
    BigDecimal open =
        parseDecimal(
            sourceName, lineNumber, COLUMN_PRICE_OPEN, cells[columnIndices.get(COLUMN_PRICE_OPEN)]);
    BigDecimal high =
        parseDecimal(
            sourceName, lineNumber, COLUMN_PRICE_HIGH, cells[columnIndices.get(COLUMN_PRICE_HIGH)]);
    BigDecimal low =
        parseDecimal(
            sourceName, lineNumber, COLUMN_PRICE_LOW, cells[columnIndices.get(COLUMN_PRICE_LOW)]);
    BigDecimal close =
        parseDecimal(
            sourceName,
            lineNumber,
            COLUMN_PRICE_CLOSE,
            cells[columnIndices.get(COLUMN_PRICE_CLOSE)]);
    BigDecimal volume =
        parseDecimal(
            sourceName, lineNumber, COLUMN_VOLUME, cells[columnIndices.get(COLUMN_VOLUME)]);
    return new PriceBar(priceDate, open, high, low, close, volume);
  }

  private static void recordExpectedColumns(
      String sourceName,
      int lineNumber,
      LocalDate priceDate,
      List<String> expectedColumnNames,
      Map<String, Integer> columnIndices,
      String[] cells,
      Map<String, Map<LocalDate, BigDecimal>> expectedColumns) {
    for (String columnName : expectedColumnNames) {
      String cellValue = cells[columnIndices.get(columnName)];
      if (StringUtils.isNotBlank(cellValue)) {
        BigDecimal parsedValue = parseDecimal(sourceName, lineNumber, columnName, cellValue);
        expectedColumns.get(columnName).put(priceDate, parsedValue);
      }
    }
  }

  private static LocalDate parseDate(
      String sourceName, int lineNumber, String columnName, String rawValue) {
    String trimmed = rawValue.trim();
    if (StringUtils.isNotBlank(trimmed)) {
      try {
        return LocalDate.parse(trimmed);
      } catch (DateTimeParseException parseException) {
        throw new IllegalArgumentException(
            "Invalid date '"
                + trimmed
                + "' in column '"
                + columnName
                + "' at "
                + sourceName
                + ":"
                + lineNumber,
            parseException);
      }
    } else {
      throw new IllegalArgumentException(
          "Blank date in column '" + columnName + "' at " + sourceName + ":" + lineNumber);
    }
  }

  private static BigDecimal parseDecimal(
      String sourceName, int lineNumber, String columnName, String rawValue) {
    String trimmed = rawValue.trim();
    if (StringUtils.isNotBlank(trimmed)) {
      try {
        return new BigDecimal(trimmed);
      } catch (NumberFormatException numberFormatException) {
        throw new IllegalArgumentException(
            "Invalid decimal '"
                + trimmed
                + "' in column '"
                + columnName
                + "' at "
                + sourceName
                + ":"
                + lineNumber,
            numberFormatException);
      }
    } else {
      throw new IllegalArgumentException(
          "Blank decimal in column '" + columnName + "' at " + sourceName + ":" + lineNumber);
    }
  }
}
