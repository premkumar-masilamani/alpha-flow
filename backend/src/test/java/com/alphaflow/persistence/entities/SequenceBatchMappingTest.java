package com.alphaflow.persistence.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SequenceBatchMappingTest {

  private static final int EXPECTED_ALLOCATION_SIZE = 50;
  private static final int POSTGRES_MAX_IDENTIFIER_BYTES = 63;
  private static final String APPLICATION_PROPERTIES_RESOURCE = "/application.properties";
  private static final Path UP_MIGRATION_PATH =
      Path.of("../database/migrations/036_enable_sequence_batch_allocation.up.sql");
  private static final Path DOWN_MIGRATION_PATH =
      Path.of("../database/migrations/036_enable_sequence_batch_allocation.down.sql");

  private static final List<String> LEGACY_SEQUENCE_RENAMES_UP =
      List.of(
          "ALTER SEQUENCE IF EXISTS public.daily_prices_id_seq RENAME TO"
              + " daily_prices_daily_price_id_seq;",
          "ALTER SEQUENCE IF EXISTS public.weekly_prices_id_seq RENAME TO"
              + " weekly_prices_weekly_price_id_seq;",
          "ALTER SEQUENCE IF EXISTS public.daily_candlestick_patterns_id_seq RENAME TO"
              + " daily_candlestick_patterns_daily_candlestick_pattern_id_seq;",
          "ALTER SEQUENCE IF EXISTS public.weekly_candlestick_patterns_id_seq RENAME TO"
              + " weekly_candlestick_patterns_weekly_candlestick_pattern_id_seq;");

  private static final List<String> LEGACY_SEQUENCE_RENAMES_DOWN =
      List.of(
          "ALTER SEQUENCE IF EXISTS public.daily_prices_daily_price_id_seq RENAME TO"
              + " daily_prices_id_seq;",
          "ALTER SEQUENCE IF EXISTS public.weekly_prices_weekly_price_id_seq RENAME TO"
              + " weekly_prices_id_seq;",
          "ALTER SEQUENCE IF EXISTS"
              + " public.daily_candlestick_patterns_daily_candlestick_pattern_id_seq RENAME TO"
              + " daily_candlestick_patterns_id_seq;",
          "ALTER SEQUENCE IF EXISTS"
              + " public.weekly_candlestick_patterns_weekly_candlestick_pattern_id_seq RENAME TO"
              + " weekly_candlestick_patterns_id_seq;");

  private static Stream<Arguments> highVolumeEngineEntities() {
    return Stream.of(
        Arguments.of(DailyPrice.class, "daily_prices", "daily_price_id"),
        Arguments.of(WeeklyPrice.class, "weekly_prices", "weekly_price_id"),
        Arguments.of(DailyIndicator.class, "daily_indicators", "daily_indicator_id"),
        Arguments.of(WeeklyIndicator.class, "weekly_indicators", "weekly_indicator_id"),
        Arguments.of(
            DailySupportResistance.class,
            "daily_support_resistances",
            "daily_support_resistance_id"),
        Arguments.of(
            WeeklySupportResistance.class,
            "weekly_support_resistances",
            "weekly_support_resistance_id"),
        Arguments.of(
            DailyCandlestickPattern.class,
            "daily_candlestick_patterns",
            "daily_candlestick_pattern_id"),
        Arguments.of(
            WeeklyCandlestickPattern.class,
            "weekly_candlestick_patterns",
            "weekly_candlestick_pattern_id"),
        Arguments.of(DailyChartPattern.class, "daily_chart_patterns", "daily_chart_pattern_id"),
        Arguments.of(WeeklyChartPattern.class, "weekly_chart_patterns", "weekly_chart_pattern_id"));
  }

  private static Stream<Class<?>> lowVolumeEntities() {
    return Stream.of(Ticker.class, IndicatorDefinition.class);
  }

  @ParameterizedTest
  @MethodSource("highVolumeEngineEntities")
  void highVolumeEntityUsesSequenceGeneratorWithAllocationSizeFifty(
      Class<?> entityClass, String tableName, String pkColumnName) {
    Field idField = findIdField(entityClass);
    String expectedGeneratorName = tableName + "_seq_gen";

    GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
    assertNotNull(generatedValue, entityClass.getSimpleName() + " must have @GeneratedValue");
    assertEquals(GenerationType.SEQUENCE, generatedValue.strategy());
    assertEquals(expectedGeneratorName, generatedValue.generator());

    SequenceGenerator sequenceGenerator = idField.getAnnotation(SequenceGenerator.class);
    assertNotNull(sequenceGenerator, entityClass.getSimpleName() + " must have @SequenceGenerator");
    assertEquals(expectedGeneratorName, sequenceGenerator.name());
    String expectedSequenceName = tableName + "_" + pkColumnName + "_seq";
    assertEquals(expectedSequenceName, sequenceGenerator.sequenceName());
    assertEquals(EXPECTED_ALLOCATION_SIZE, sequenceGenerator.allocationSize());
    assertTrue(
        expectedSequenceName.getBytes(StandardCharsets.UTF_8).length
            <= POSTGRES_MAX_IDENTIFIER_BYTES,
        "Sequence name exceeds PostgreSQL 63-byte limit: " + expectedSequenceName);
  }

  @ParameterizedTest
  @MethodSource("lowVolumeEntities")
  void lowVolumeEntityRetainsIdentityStrategyWithoutSequenceGenerator(Class<?> entityClass) {
    Field idField = findIdField(entityClass);

    GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
    assertNotNull(generatedValue, entityClass.getSimpleName() + " must have @GeneratedValue");
    assertEquals(GenerationType.IDENTITY, generatedValue.strategy());
    assertNull(
        idField.getAnnotation(SequenceGenerator.class),
        entityClass.getSimpleName() + " must not declare @SequenceGenerator");
  }

  @Test
  void applicationPropertiesEnablesBatchingAndRewriteBatchedInserts() throws IOException {
    Properties properties = new Properties();
    try (InputStream resourceStream =
        SequenceBatchMappingTest.class.getResourceAsStream(APPLICATION_PROPERTIES_RESOURCE)) {
      assertNotNull(resourceStream, "application.properties must exist on classpath");
      try (InputStreamReader reader =
          new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
        properties.load(reader);
      }
    }

    assertEquals(
        "jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"
            + "?reWriteBatchedInserts=true",
        properties.getProperty("spring.datasource.url"));
    assertEquals("100", properties.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"));
    assertEquals("true", properties.getProperty("spring.jpa.properties.hibernate.order_inserts"));
    assertEquals("true", properties.getProperty("spring.jpa.properties.hibernate.order_updates"));
    assertEquals(
        "true",
        properties.getProperty("spring.jpa.properties.hibernate.jdbc.batch_versioned_data"));
  }

  @Test
  void upMigrationRenamesLegacySequencesAndSetsIncrementByFiftyOnAllEngineTables()
      throws IOException {
    assertTrue(Files.exists(UP_MIGRATION_PATH), "Migration 036 up SQL must exist");
    String normalizedSql =
        normalizeWhitespace(Files.readString(UP_MIGRATION_PATH, StandardCharsets.UTF_8));

    for (String renameStatement : LEGACY_SEQUENCE_RENAMES_UP) {
      assertTrue(
          normalizedSql.contains(renameStatement), "Up migration must contain: " + renameStatement);
    }

    highVolumeEngineEntities()
        .forEach(arguments -> {
          String tableName = (String) arguments.get()[1];
          String pkColumnName = (String) arguments.get()[2];
          String expectedAlter =
              "ALTER TABLE public."
                  + tableName
                  + " ALTER COLUMN "
                  + pkColumnName
                  + " SET INCREMENT BY 50;";
          assertTrue(
              normalizedSql.contains(expectedAlter), "Up migration must contain: " + expectedAlter);
        });
  }

  @Test
  void downMigrationRevertsIncrementByOneAndRestoresLegacySequenceNames() throws IOException {
    assertTrue(Files.exists(DOWN_MIGRATION_PATH), "Migration 036 down SQL must exist");
    String normalizedSql =
        normalizeWhitespace(Files.readString(DOWN_MIGRATION_PATH, StandardCharsets.UTF_8));

    highVolumeEngineEntities()
        .forEach(arguments -> {
          String tableName = (String) arguments.get()[1];
          String pkColumnName = (String) arguments.get()[2];
          String expectedAlter =
              "ALTER TABLE public."
                  + tableName
                  + " ALTER COLUMN "
                  + pkColumnName
                  + " SET INCREMENT BY 1;";
          assertTrue(
              normalizedSql.contains(expectedAlter),
              "Down migration must contain: " + expectedAlter);
        });

    for (String renameStatement : LEGACY_SEQUENCE_RENAMES_DOWN) {
      assertTrue(
          normalizedSql.contains(renameStatement),
          "Down migration must contain: " + renameStatement);
    }
  }

  private static Field findIdField(Class<?> entityClass) {
    return Arrays.stream(entityClass.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(Id.class))
        .findFirst()
        .orElseThrow(() -> new AssertionError(entityClass.getSimpleName() + " has no @Id field"));
  }

  private static String normalizeWhitespace(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }
}
