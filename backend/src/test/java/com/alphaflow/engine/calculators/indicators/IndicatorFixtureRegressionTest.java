package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.fixtures.CsvFixtureLoader;
import com.alphaflow.fixtures.MarketDataFixture;
import com.alphaflow.persistence.enums.IndicatorType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IndicatorFixtureRegressionTest {

  private static final String DAILY_FIXTURE_PATH = "fixtures/aapl_daily.csv";
  private static final String WEEKLY_FIXTURE_PATH = "fixtures/aapl_weekly.csv";
  private static final String INDICATOR_DEFINITIONS_PATH = "fixtures/indicator_definitions.json";

  private static MarketDataFixture dailyFixture;
  private static MarketDataFixture weeklyFixture;
  private static List<IndicatorFixtureCase> indicatorCases;

  @BeforeAll
  static void loadFixtures() {
    dailyFixture = CsvFixtureLoader.load(DAILY_FIXTURE_PATH);
    weeklyFixture = CsvFixtureLoader.load(WEEKLY_FIXTURE_PATH);
    indicatorCases = IndicatorDefinitionFixtureLoader.load(INDICATOR_DEFINITIONS_PATH);
  }

  private static Stream<Arguments> fixtureAndIndicatorCases() {
    Map<Timeframe, MarketDataFixture> fixturesByTimeframe =
        Map.of(
            Timeframe.DAILY, dailyFixture,
            Timeframe.WEEKLY, weeklyFixture);
    List<Timeframe> orderedTimeframes = List.of(Timeframe.DAILY, Timeframe.WEEKLY);

    return orderedTimeframes.stream()
        .flatMap(timeframe -> {
          MarketDataFixture fixture = fixturesByTimeframe.get(timeframe);
          return indicatorCases.stream()
              .map(
                  fixtureCase -> Arguments.of(timeframe, fixtureCase.name(), fixture, fixtureCase));
        });
  }

  @ParameterizedTest(name = "{0} :: {1}")
  @MethodSource("fixtureAndIndicatorCases")
  void indicatorMatchesFixtureGoldenValues(
      Timeframe timeframe,
      String caseName,
      MarketDataFixture fixture,
      IndicatorFixtureCase fixtureCase) {
    String fixtureLabel = timeframe + " (" + fixture.resourcePath() + ")";
    Map<LocalDate, Map<String, BigDecimal>> computed =
        fixtureCase.indicator().compute(fixture.bars(), fixtureCase.params(), fixtureCase.source());

    assertFalse(
        computed.isEmpty(),
        () -> "Computed indicator series was empty for " + fixtureLabel + " :: " + caseName);

    Set<LocalDate> expectedUnionDates = new LinkedHashSet<>();
    for (String columnName : fixtureCase.outputKeyToColumn().values()) {
      expectedUnionDates.addAll(fixture.expectedSeries(columnName).keySet());
    }
    assertEquals(
        expectedUnionDates,
        computed.keySet(),
        () -> "Computed date set mismatch for " + fixtureLabel + " :: " + caseName);

    for (Map.Entry<String, String> mapping : fixtureCase.outputKeyToColumn().entrySet()) {
      String outputKey = mapping.getKey();
      String columnName = mapping.getValue();
      Map<LocalDate, BigDecimal> expectedSeries = fixture.expectedSeries(columnName);

      Set<LocalDate> actualOutputDates = new LinkedHashSet<>();
      for (Map.Entry<LocalDate, Map<String, BigDecimal>> barEntry : computed.entrySet()) {
        if (barEntry.getValue().containsKey(outputKey)) {
          actualOutputDates.add(barEntry.getKey());
        }
      }

      assertEquals(
          expectedSeries.keySet(),
          actualOutputDates,
          () ->
              "Output key date set mismatch for "
                  + fixtureLabel
                  + " :: "
                  + caseName
                  + " key="
                  + outputKey);
    }

    int totalValueChecks = 0;
    for (Map.Entry<LocalDate, Map<String, BigDecimal>> barEntry : computed.entrySet()) {
      LocalDate date = barEntry.getKey();
      Map<String, BigDecimal> actualBarValues = barEntry.getValue();

      Set<String> expectedKeysForDate = new LinkedHashSet<>();
      for (Map.Entry<String, String> mapping : fixtureCase.outputKeyToColumn().entrySet()) {
        if (fixture.expectedSeries(mapping.getValue()).containsKey(date)) {
          expectedKeysForDate.add(mapping.getKey());
        }
      }

      assertEquals(
          expectedKeysForDate,
          actualBarValues.keySet(),
          () -> "Output keys mismatch for " + fixtureLabel + " :: " + caseName + " date=" + date);

      for (Map.Entry<String, BigDecimal> valueEntry : actualBarValues.entrySet()) {
        String outputKey = valueEntry.getKey();
        BigDecimal actualValue = valueEntry.getValue();
        String columnName = fixtureCase.outputKeyToColumn().get(outputKey);
        BigDecimal expectedValue = fixture.expectedSeries(columnName).get(date);

        assertEquals(
            0,
            actualValue.compareTo(expectedValue),
            () ->
                "Value mismatch for fixture="
                    + fixtureLabel
                    + " case="
                    + caseName
                    + " date="
                    + date
                    + " key="
                    + outputKey
                    + " expected="
                    + expectedValue
                    + " actual="
                    + actualValue);
        totalValueChecks++;
      }
    }

    int totalBars = fixture.bars().size();
    int computedBars = computed.size();
    int warmupBars = totalBars - computedBars;
    LocalDate firstDate = fixture.bars().getFirst().date();
    LocalDate lastDate = fixture.bars().getLast().date();
    System.out.println(
        "[INDICATOR-FIXTURE] Timeframe="
            + timeframe
            + " | Indicator="
            + caseName
            + " | Source="
            + fixtureCase.source()
            + " | Params="
            + fixtureCase.params().canonical()
            + " | Bars="
            + computedBars
            + "/"
            + totalBars
            + " computed ("
            + warmupBars
            + " warm-up blank, "
            + firstDate
            + ".."
            + lastDate
            + ")"
            + " | OutputKeys="
            + fixtureCase.outputKeyToColumn().keySet()
            + " | ValueAssertions="
            + totalValueChecks
            + " | PASS");
  }

  @Test
  void allIndicatorTypesAreCoveredByFixtures() {
    Set<IndicatorType> coveredTypes =
        indicatorCases.stream()
            .map(fixtureCase -> fixtureCase.indicator().type())
            .collect(Collectors.toSet());

    assertEquals(
        EnumSet.allOf(IndicatorType.class),
        coveredTypes,
        "Every IndicatorType must be covered by at least one IndicatorFixtureCase");
  }

  @Test
  void loadedIndicatorDefinitionsAreImmutableAndLoaderFailsFast() {
    assertThrows(UnsupportedOperationException.class, indicatorCases::clear);
    assertThrows(
        IllegalStateException.class,
        () -> IndicatorDefinitionFixtureLoader.load("fixtures/does_not_exist.json"));
    assertThrows(
        IllegalArgumentException.class, () -> IndicatorDefinitionFixtureLoader.load("   "));
  }
}
