package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.alphaflow.engine.indicators.BollingerBandsIndicator;
import com.alphaflow.engine.indicators.EmaIndicator;
import com.alphaflow.engine.indicators.MacdIndicator;
import com.alphaflow.engine.indicators.RsiIndicator;
import com.alphaflow.engine.indicators.SmaIndicator;
import com.alphaflow.engine.indicators.StochasticIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.fixtures.CsvFixtureLoader;
import com.alphaflow.fixtures.MarketDataFixture;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
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

  private static MarketDataFixture dailyFixture;
  private static MarketDataFixture weeklyFixture;

  @BeforeAll
  static void loadFixtures() {
    dailyFixture = CsvFixtureLoader.load(DAILY_FIXTURE_PATH);
    weeklyFixture = CsvFixtureLoader.load(WEEKLY_FIXTURE_PATH);
  }

  private static List<IndicatorFixtureCase> indicatorCases() {
    return List.of(
        new IndicatorFixtureCase(
            "SMA(CLOSE, 20)",
            new SmaIndicator(),
            IndicatorParams.parse("period=20"),
            PriceSource.CLOSE,
            Map.of(IndicatorOutputKey.VALUE.getValue(), "sma_close_20")),
        new IndicatorFixtureCase(
            "SMA(VOLUME, 20)",
            new SmaIndicator(),
            IndicatorParams.parse("period=20"),
            PriceSource.VOLUME,
            Map.of(IndicatorOutputKey.VALUE.getValue(), "sma_volume_20")),
        new IndicatorFixtureCase(
            "EMA(CLOSE, 5)",
            new EmaIndicator(),
            IndicatorParams.parse("period=5"),
            PriceSource.CLOSE,
            Map.of(IndicatorOutputKey.VALUE.getValue(), "ema_close_5")),
        new IndicatorFixtureCase(
            "EMA(CLOSE, 13)",
            new EmaIndicator(),
            IndicatorParams.parse("period=13"),
            PriceSource.CLOSE,
            Map.of(IndicatorOutputKey.VALUE.getValue(), "ema_close_13")),
        new IndicatorFixtureCase(
            "EMA(CLOSE, 26)",
            new EmaIndicator(),
            IndicatorParams.parse("period=26"),
            PriceSource.CLOSE,
            Map.of(IndicatorOutputKey.VALUE.getValue(), "ema_close_26")),
        new IndicatorFixtureCase(
            "RSI(CLOSE, 14)",
            new RsiIndicator(),
            IndicatorParams.parse("period=14"),
            PriceSource.CLOSE,
            Map.of(IndicatorOutputKey.VALUE.getValue(), "rsi_close_14")),
        new IndicatorFixtureCase(
            "MACD(CLOSE, 12/26/9)",
            new MacdIndicator(),
            IndicatorParams.parse("fast=12,slow=26,signal=9"),
            PriceSource.CLOSE,
            Map.of(
                IndicatorOutputKey.MACD.getValue(), "macd_close_12_26_9_macd",
                IndicatorOutputKey.SIGNAL.getValue(), "macd_close_12_26_9_signal",
                IndicatorOutputKey.HISTOGRAM.getValue(), "macd_close_12_26_9_histogram")),
        new IndicatorFixtureCase(
            "STOCHASTIC(CLOSE, 14/3/3)",
            new StochasticIndicator(),
            IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"),
            PriceSource.CLOSE,
            Map.of(
                IndicatorOutputKey.K.getValue(), "stochastic_close_14_3_3_k",
                IndicatorOutputKey.D.getValue(), "stochastic_close_14_3_3_d")),
        new IndicatorFixtureCase(
            "BB(CLOSE, 20, 2)",
            new BollingerBandsIndicator(),
            IndicatorParams.parse("period=20,stdDev=2"),
            PriceSource.CLOSE,
            Map.of(
                IndicatorOutputKey.UPPER.getValue(), "bb_close_20_2_upper",
                IndicatorOutputKey.MIDDLE.getValue(), "bb_close_20_2_middle",
                IndicatorOutputKey.LOWER.getValue(), "bb_close_20_2_lower",
                IndicatorOutputKey.BANDWIDTH.getValue(), "bb_close_20_2_bandwidth")));
  }

  private static Stream<Arguments> fixtureAndIndicatorCases() {
    List<MarketDataFixture> fixtures = List.of(dailyFixture, weeklyFixture);
    return fixtures.stream()
        .flatMap(
            fixture ->
                indicatorCases().stream()
                    .map(
                        fixtureCase ->
                            Arguments.of(
                                fixture.resourcePath(), fixtureCase.name(), fixture, fixtureCase)));
  }

  @ParameterizedTest(name = "{0} :: {1}")
  @MethodSource("fixtureAndIndicatorCases")
  void indicatorMatchesFixtureGoldenValues(
      String fixtureLabel,
      String caseName,
      MarketDataFixture fixture,
      IndicatorFixtureCase fixtureCase) {
    Map<LocalDate, Map<String, BigDecimal>> computed =
        fixtureCase
            .indicator()
            .compute(fixture.bars(), fixtureCase.params(), fixtureCase.source());

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
          () ->
              "Output keys mismatch for "
                  + fixtureLabel
                  + " :: "
                  + caseName
                  + " date="
                  + date);

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
      }
    }
  }

  @Test
  void allIndicatorTypesAreCoveredByFixtures() {
    Set<IndicatorType> coveredTypes =
        indicatorCases().stream()
            .map(fixtureCase -> fixtureCase.indicator().type())
            .collect(Collectors.toSet());

    assertEquals(
        EnumSet.allOf(IndicatorType.class),
        coveredTypes,
        "Every IndicatorType must be covered by at least one IndicatorFixtureCase");
  }
}
