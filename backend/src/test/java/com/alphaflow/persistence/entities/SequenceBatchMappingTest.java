package com.alphaflow.persistence.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SequenceBatchMappingTest {

  private static final int POSTGRES_MAX_IDENTIFIER_BYTES = 63;
  private static final String EXPECTED_DATASOURCE_URL =
      "jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}?reWriteBatchedInserts=true";
  private static final String EXPECTED_BATCH_SIZE = "100";
  private static final Path UP_MIGRATION_PATH =
      Path.of("..", "database", "migrations", "036_enable_sequence_batch_allocation.up.sql");
  private static final Path DOWN_MIGRATION_PATH =
      Path.of("..", "database", "migrations", "036_enable_sequence_batch_allocation.down.sql");

  static Stream<Arguments> highVolumeEngineEntities() {
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

  static Stream<Arguments> lowVolumeMetadataEntities() {
    return Stream.of(Arguments.of(Ticker.class), Arguments.of(IndicatorDefinition.class));
  }

  @ParameterizedTest
  @MethodSource("highVolumeEngineEntities")
  void highVolumeEntityUsesDefaultSequenceStrategy(
      Class<?> entityClass, String expectedTableName, String expectedPkColumn) {
    Table tableAnnotation = entityClass.getAnnotation(Table.class);
    assertNotNull(tableAnnotation, "Missing @Table on " + entityClass.getSimpleName());
    assertEquals(expectedTableName, tableAnnotation.name());

    Field idField = findIdField(entityClass);
    Column columnAnnotation = idField.getAnnotation(Column.class);
    assertNotNull(columnAnnotation, "Missing @Column on @Id of " + entityClass.getSimpleName());
    assertEquals(expectedPkColumn, columnAnnotation.name());

    GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
    assertNotNull(generatedValue, "Missing @GeneratedValue on " + entityClass.getSimpleName());
    assertEquals(GenerationType.SEQUENCE, generatedValue.strategy());
    assertEquals("", generatedValue.generator());

    SequenceGenerator sequenceGenerator = idField.getAnnotation(SequenceGenerator.class);
    assertNull(
        sequenceGenerator,
        "@SequenceGenerator should be omitted on "
            + entityClass.getSimpleName()
            + " to rely on Hibernate's default <table>_seq naming and allocationSize=50");

    String defaultSequenceName = expectedTableName + "_seq";
    assertTrue(
        defaultSequenceName.getBytes(StandardCharsets.UTF_8).length
            <= POSTGRES_MAX_IDENTIFIER_BYTES,
        "Sequence name exceeds PostgreSQL 63-byte limit: " + defaultSequenceName);
  }

  @ParameterizedTest
  @MethodSource("lowVolumeMetadataEntities")
  void lowVolumeEntityRemainsIdentityStrategy(Class<?> entityClass) {
    Field idField = findIdField(entityClass);
    GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
    assertNotNull(generatedValue, "Missing @GeneratedValue on " + entityClass.getSimpleName());
    assertEquals(GenerationType.IDENTITY, generatedValue.strategy());
    assertNull(
        idField.getAnnotation(SequenceGenerator.class),
        "Low-volume entity should not declare @SequenceGenerator: " + entityClass.getSimpleName());
  }

  @Test
  void applicationPropertiesEnablesBatchingAndRewriteBatchedInserts() throws IOException {
    Properties properties = new Properties();
    try (InputStream resourceStream =
        SequenceBatchMappingTest.class.getClassLoader().getResourceAsStream("application.properties")) {
      assertNotNull(resourceStream, "application.properties must exist on classpath");
      try (InputStreamReader reader =
          new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
        properties.load(reader);
      }
    }

    assertEquals(EXPECTED_DATASOURCE_URL, properties.getProperty("spring.datasource.url"));
    assertEquals(
        EXPECTED_BATCH_SIZE,
        properties.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"));
    assertEquals("true", properties.getProperty("spring.jpa.properties.hibernate.order_inserts"));
    assertEquals("true", properties.getProperty("spring.jpa.properties.hibernate.order_updates"));
    assertEquals(
        "true",
        properties.getProperty("spring.jpa.properties.hibernate.jdbc.batch_versioned_data"));
  }

  @Test
  void upMigrationSwitchesAllTenTablesFromIdentityToSequences() throws IOException {
    assertTrue(Files.exists(UP_MIGRATION_PATH), "Missing up migration: " + UP_MIGRATION_PATH);
    String upSql = Files.readString(UP_MIGRATION_PATH, StandardCharsets.UTF_8);

    assertFalse(upSql.contains("ALTER SEQUENCE"), "Up migration must not contain ALTER SEQUENCE");

    highVolumeEngineEntities()
        .forEach(
            arguments -> {
              String tableName = (String) arguments.get()[1];
              String pkColumn = (String) arguments.get()[2];
              String sequenceName = tableName + "_seq";
              assertTrue(
                  upSql.contains(
                      "ALTER TABLE public."
                          + tableName
                          + " ALTER COLUMN "
                          + pkColumn
                          + " DROP IDENTITY IF EXISTS;"),
                  "Up migration missing DROP IDENTITY for " + tableName);
              assertTrue(
                  upSql.contains(
                      "CREATE SEQUENCE IF NOT EXISTS public."
                          + sequenceName
                          + " START WITH 1 INCREMENT BY 50 OWNED BY public."
                          + tableName
                          + "."
                          + pkColumn
                          + ";"),
                  "Up migration missing CREATE SEQUENCE for " + sequenceName);
              assertTrue(
                  upSql.contains(
                      "ALTER TABLE public."
                          + tableName
                          + " ALTER COLUMN "
                          + pkColumn
                          + " SET DEFAULT nextval('public."
                          + sequenceName
                          + "');"),
                  "Up migration missing SET DEFAULT nextval for " + tableName);
            });
  }

  @Test
  void downMigrationRevertsAllTenTablesFromSequencesToIdentity() throws IOException {
    assertTrue(Files.exists(DOWN_MIGRATION_PATH), "Missing down migration: " + DOWN_MIGRATION_PATH);
    String downSql = Files.readString(DOWN_MIGRATION_PATH, StandardCharsets.UTF_8);

    assertFalse(
        downSql.contains("ALTER SEQUENCE"), "Down migration must not contain ALTER SEQUENCE");

    highVolumeEngineEntities()
        .forEach(
            arguments -> {
              String tableName = (String) arguments.get()[1];
              String pkColumn = (String) arguments.get()[2];
              String sequenceName = tableName + "_seq";
              assertTrue(
                  downSql.contains(
                      "ALTER TABLE public."
                          + tableName
                          + " ALTER COLUMN "
                          + pkColumn
                          + " DROP DEFAULT;"),
                  "Down migration missing DROP DEFAULT for " + tableName);
              assertTrue(
                  downSql.contains("DROP SEQUENCE IF EXISTS public." + sequenceName + ";"),
                  "Down migration missing DROP SEQUENCE for " + sequenceName);
              assertTrue(
                  downSql.contains(
                      "ALTER TABLE public."
                          + tableName
                          + " ALTER COLUMN "
                          + pkColumn
                          + " ADD GENERATED BY DEFAULT AS IDENTITY;"),
                  "Down migration missing ADD GENERATED BY DEFAULT AS IDENTITY for " + tableName);
            });
  }

  private static Field findIdField(Class<?> entityClass) {
    for (Field field : entityClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(Id.class)) {
        return field;
      }
    }
    throw new AssertionError("No @Id field found on " + entityClass.getSimpleName());
  }
}
