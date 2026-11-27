package com.alphaflow.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvFixtureLoaderTest {

  private static final String DAILY_FIXTURE_PATH = "fixtures/aapl_daily.csv";
  private static final String WEEKLY_FIXTURE_PATH = "fixtures/aapl_weekly.csv";
  private static final int EXPECTED_DAILY_BAR_COUNT = 250;
  private static final int EXPECTED_WEEKLY_BAR_COUNT = 52;
  private static final int EXPECTED_INDICATOR_COLUMN_COUNT = 15;

  @Test
  void loadsDailyFixtureWithExpectedRowCountDateRangeAndColumns() {
    MarketDataFixture fixture = CsvFixtureLoader.load(DAILY_FIXTURE_PATH);

    assertEquals(DAILY_FIXTURE_PATH, fixture.resourcePath());
    assertEquals(EXPECTED_DAILY_BAR_COUNT, fixture.bars().size());
    assertEquals(LocalDate.of(2025, 1, 2), fixture.bars().getFirst().date());
    assertEquals(LocalDate.of(2025, 12, 31), fixture.bars().getLast().date());
    assertEquals(EXPECTED_INDICATOR_COLUMN_COUNT, fixture.columnNames().size());
    assertTrue(fixture.columnNames().contains("sma_close_20"));
    assertTrue(fixture.columnNames().contains("bb_close_20_2_bandwidth"));
  }

  @Test
  void loadsWeeklyFixtureWithExpectedRowCountDateRangeAndColumns() {
    MarketDataFixture fixture = CsvFixtureLoader.load(WEEKLY_FIXTURE_PATH);

    assertEquals(WEEKLY_FIXTURE_PATH, fixture.resourcePath());
    assertEquals(EXPECTED_WEEKLY_BAR_COUNT, fixture.bars().size());
    assertEquals(LocalDate.of(2025, 1, 6), fixture.bars().getFirst().date());
    assertEquals(LocalDate.of(2025, 12, 29), fixture.bars().getLast().date());
    assertEquals(EXPECTED_INDICATOR_COLUMN_COUNT, fixture.columnNames().size());
    assertTrue(fixture.columnNames().contains("macd_close_12_26_9_histogram"));
    assertTrue(fixture.columnNames().contains("stochastic_close_14_3_3_d"));
  }

  @Test
  void returnedFixtureIsStrictlyImmutable() {
    MarketDataFixture fixture = CsvFixtureLoader.load(DAILY_FIXTURE_PATH);
    PriceBar syntheticBar =
        new PriceBar(
            LocalDate.of(2026, 1, 2),
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.TEN);

    assertThrows(UnsupportedOperationException.class, () -> fixture.bars().add(syntheticBar));
    assertThrows(UnsupportedOperationException.class, () -> fixture.bars().clear());
    assertThrows(
        UnsupportedOperationException.class,
        () -> fixture.expectedColumns().put("injected", Map.of()));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            fixture
                .expectedSeries("sma_close_20")
                .put(LocalDate.of(2026, 1, 2), BigDecimal.valueOf(100)));
    assertThrows(UnsupportedOperationException.class, () -> fixture.columnNames().add("injected"));
  }

  @Test
  void failsFastOnMissingClasspathResource() {
    assertThrows(
        IllegalStateException.class,
        () -> CsvFixtureLoader.load("fixtures/non_existent_fixture.csv"));
  }

  @Test
  void failsFastOnBlankClasspathResource() {
    assertThrows(IllegalArgumentException.class, () -> CsvFixtureLoader.load("   "));
  }

  @Test
  void failsFastOnUnknownColumnName() {
    MarketDataFixture fixture = CsvFixtureLoader.load(DAILY_FIXTURE_PATH);

    assertThrows(IllegalArgumentException.class, () -> fixture.expectedSeries("does_not_exist"));
  }

  @Test
  void failsFastOnMissingHeader() {
    BufferedReader reader = new BufferedReader(new StringReader(""));

    assertThrows(IllegalArgumentException.class, () -> CsvFixtureLoader.load("empty.csv", reader));
  }

  @Test
  void failsFastOnMissingRequiredOhlcvColumn() {
    String csvContent =
        "price_date,price_open,price_high,price_low,price_close\n"
            + "2025-01-02,100.00,101.00,99.00,100.50\n";
    BufferedReader reader = new BufferedReader(new StringReader(csvContent));

    assertThrows(
        IllegalArgumentException.class, () -> CsvFixtureLoader.load("missing_volume.csv", reader));
  }

  @Test
  void failsFastOnColumnCountMismatch() {
    String csvContent =
        "price_date,price_open,price_high,price_low,price_close,volume,sma_close_20\n"
            + "2025-01-02,100.00,101.00,99.00,100.50,1000\n";
    BufferedReader reader = new BufferedReader(new StringReader(csvContent));

    assertThrows(
        IllegalArgumentException.class, () -> CsvFixtureLoader.load("mismatch.csv", reader));
  }

  @Test
  void failsFastOnDuplicateOrNonChronologicalDates() {
    String duplicateDateCsv =
        "price_date,price_open,price_high,price_low,price_close,volume\n"
            + "2025-01-02,100.00,101.00,99.00,100.50,1000\n"
            + "2025-01-02,100.50,102.00,100.00,101.00,1200\n";
    String outOfOrderCsv =
        "price_date,price_open,price_high,price_low,price_close,volume\n"
            + "2025-01-03,100.00,101.00,99.00,100.50,1000\n"
            + "2025-01-02,100.50,102.00,100.00,101.00,1200\n";

    assertThrows(
        IllegalArgumentException.class,
        () ->
            CsvFixtureLoader.load(
                "duplicate.csv", new BufferedReader(new StringReader(duplicateDateCsv))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            CsvFixtureLoader.load(
                "out_of_order.csv", new BufferedReader(new StringReader(outOfOrderCsv))));
  }

  @Test
  void failsFastOnUnparseableDateOrDecimal() {
    String badDateCsv =
        "price_date,price_open,price_high,price_low,price_close,volume\n"
            + "not-a-date,100.00,101.00,99.00,100.50,1000\n";
    String badNumberCsv =
        "price_date,price_open,price_high,price_low,price_close,volume\n"
            + "2025-01-02,100.00,NaN,99.00,100.50,1000\n";

    assertThrows(
        IllegalArgumentException.class,
        () -> CsvFixtureLoader.load("bad_date.csv", new BufferedReader(new StringReader(badDateCsv))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            CsvFixtureLoader.load(
                "bad_number.csv", new BufferedReader(new StringReader(badNumberCsv))));
  }

  @Test
  void failsFastOnCsvWithHeaderOnly() {
    String headerOnlyCsv = "price_date,price_open,price_high,price_low,price_close,volume\n";

    assertThrows(
        IllegalArgumentException.class,
        () ->
            CsvFixtureLoader.load(
                "header_only.csv", new BufferedReader(new StringReader(headerOnlyCsv))));
  }
}
