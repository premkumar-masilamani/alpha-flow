package com.alphaflow.engine.calculators.indicators;


import com.alphaflow.persistence.enums.PriceSource;


import java.io.BufferedReader;

import java.io.InputStreamReader;

import java.math.BigDecimal;

import java.nio.charset.StandardCharsets;

import java.time.LocalDate;

import java.util.ArrayList;

import java.util.List;


import static org.junit.jupiter.api.Assertions.*;


public final class IndicatorTestHelper {


  public static final LocalDate EPOCH = LocalDate.of(2020, 1, 1);


  private IndicatorTestHelper() {

  }


  /**

   * Deterministic, non-trivial walk so resume tests exercise real recurrence (no RNG).

   */

  public static List<PriceBar> walk(int n) {

    List<PriceBar> bars = new ArrayList<>(n);

    for (int i = 0; i < n; i++) {

      double close = 100 + 12 * Math.sin(i / 5.0) + 0.3 * i + 4 * Math.sin(i / 1.7);

      bars.add(barAt(i, close, close + 2, close - 2, close));

    }

    return bars;

  }


  public static List<PriceBar> closes(double... closes) {

    List<PriceBar> bars = new ArrayList<>(closes.length);

    for (int i = 0; i < closes.length; i++) {

      bars.add(barAt(i, closes[i], closes[i], closes[i], closes[i]));

    }

    return bars;

  }


  public static PriceBar barAt(int dayOffset, double open, double high, double low, double close) {

    return new PriceBar(

        EPOCH.plusDays(dayOffset),

        bd(open), bd(high), bd(low), bd(close),

        BigDecimal.valueOf(1000L + dayOffset));

  }


  public static BigDecimal bd(double v) {

    return BigDecimal.valueOf(v).setScale(4, IndicatorMath.ROUNDING);

  }


  public static List<PlotPoint> from(List<PlotPoint> values, LocalDate fromInclusive) {

    return values.stream().filter(p -> !p.date().isBefore(fromInclusive)).toList();

  }


  public static PlotPoint plot(List<PlotPoint> values, LocalDate date, String output) {

    return values.stream()

        .filter(p -> p.date().equals(date) && p.outputName().equals(output))

        .findFirst().orElseThrow(() -> new IllegalArgumentException("No plot point found for " + date + " " + output));

  }


  /**

   * Asserts resume-from-checkpoint reproduces a full backfill for a recursive indicator.

   */

  public static void assertRecursiveResumeMatchesBackfill(Indicator indicator, IndicatorParams params, int... splits) {

    List<PriceBar> all = walk(100);

    IndicatorResult full = indicator.compute(all, null, params, PriceSource.CLOSE);

    for (int split : splits) {

      IndicatorResult head = indicator.compute(all.subList(0, split), null, params, PriceSource.CLOSE);

      assertNotNull(head.newStateJson(), "checkpoint state must exist past warm-up at split " + split);

      IndicatorResult resumed = indicator.compute(

          all.subList(split, all.size()), head.newStateJson(), params, PriceSource.CLOSE);

      assertEquals(from(full.values(), all.get(split).date()), resumed.values(),

          "resume must equal backfill from split " + split);

    }

  }


  /**

   * Asserts a windowed indicator yields identical values over a sufficient sub-window.

   */

  public static void assertWindowedResumeMatchesBackfill(Indicator indicator, IndicatorParams params, int lookback, int... splits) {

    List<PriceBar> all = walk(100);

    IndicatorResult full = indicator.compute(all, null, params, PriceSource.CLOSE);

    for (int split : splits) {

      IndicatorResult resumed = indicator.compute(

          all.subList(split - lookback, all.size()), null, params, PriceSource.CLOSE);

      assertNull(resumed.newStateJson(), "windowed indicator carries no state");

      assertEquals(from(full.values(), all.get(split).date()),

          from(resumed.values(), all.get(split).date()),

          "windowed resume must equal backfill from split " + split);

    }

  }


  /**

   * Helper to load AAPL stock daily price data from resources (aapl.csv).

   */

  public static List<PriceBar> loadAaplCsv() {

    List<PriceBar> bars = new ArrayList<>();

    try (var is = IndicatorTestHelper.class.getResourceAsStream("/aapl.csv")) {

      if (is == null) {

        throw new IllegalStateException("Could not find /aapl.csv on the classpath");

      }

      try (var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

        String line = reader.readLine(); // Skip header

        while ((line = reader.readLine()) != null) {

          if (line.isBlank()) {

            continue;

          }

          String[] parts = line.split(",");

          if (parts.length < 6) {

            continue;

          }

          LocalDate date = LocalDate.parse(parts[0]);

          BigDecimal open = new BigDecimal(parts[1]);

          BigDecimal high = new BigDecimal(parts[2]);

          BigDecimal low = new BigDecimal(parts[3]);

          BigDecimal close = new BigDecimal(parts[4]);

          BigDecimal volume = new BigDecimal(parts[5]);

          bars.add(new PriceBar(date, open, high, low, close, volume));

        }

      }

    } catch (Exception e) {

      throw new RuntimeException("Failed to read aapl.csv from resources", e);

    }

    return bars;

  }

}

