package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.alphaflow.engine.calculators.indicators.*;
import com.alphaflow.engine.configs.IndicatorProperties;
import com.alphaflow.engine.configs.IndicatorProperties.IndicatorDefinition;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IndicatorState;
import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorStateRepository;
import com.alphaflow.persistence.repositories.IndicatorValueRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies the {@link IndicatorTickerProcessor} orchestration — the riskiest new logic — with
 * mocked
 *
 * <p>repositories: cold-start backfill writes the whole series and checkpoints the second-to-last
 * bar;
 *
 * <p>steady-state resume rewrites only the in-progress last bar and leaves the checkpoint where it
 * was.
 */
class IndicatorTickerProcessorTest {

  private static final LocalDate EPOCH = LocalDate.of(2021, 1, 4); // a Monday

  private static final int N = 12;

  private DailyPriceRepository dailyRepo;

  private WeeklyPriceRepository weeklyRepo;

  private IndicatorValueRepository valueRepo;

  private IndicatorStateRepository stateRepo;

  private IndicatorTickerProcessor processor;

  private Ticker ticker;

  private static IndicatorDefinition emaDef() {

    IndicatorDefinition d = new IndicatorDefinition();

    d.setType(IndicatorType.EMA);

    d.setSource(PriceSource.CLOSE);

    d.setParams(Map.of("period", 3));

    return d;
  }

  private static IndicatorDefinition smaVolumeDef() {

    IndicatorDefinition d = new IndicatorDefinition();

    d.setType(IndicatorType.SMA);

    d.setSource(PriceSource.VOLUME);

    d.setParams(Map.of("period", 2));

    return d;
  }

  private static LocalDate date(int index) {

    return EPOCH.plusDays(index);
  }

  private static List<DailyPrice> dailyBars() {

    List<DailyPrice> bars = new ArrayList<>(N);

    for (int i = 0; i < N; i++) {

      BigDecimal close = BigDecimal.valueOf(10 + i).setScale(4);

      bars.add(
          DailyPrice.builder()
              .priceDate(date(i))
              .priceOpen(close)
              .priceHigh(close.add(BigDecimal.ONE))
              .priceLow(close.subtract(BigDecimal.ONE))
              .priceClose(close)
              .volume(100L + i)
              .build());
    }

    return bars;
  }

  // ---- helpers --------------------------------------------------------

  private static List<PriceBar> toPriceBars(List<DailyPrice> entities) {

    return entities.stream()
        .map(
            d ->
                new PriceBar(
                    d.getPriceDate(),
                    d.getPriceOpen(),
                    d.getPriceHigh(),
                    d.getPriceLow(),
                    d.getPriceClose(),
                    BigDecimal.valueOf(d.getVolume())))
        .toList();
  }

  @BeforeEach
  void setUp() {

    dailyRepo = mock(DailyPriceRepository.class);

    weeklyRepo = mock(WeeklyPriceRepository.class);

    valueRepo = mock(IndicatorValueRepository.class);

    stateRepo = mock(IndicatorStateRepository.class);

    IndicatorRegistry registry =
        new IndicatorRegistry(
            List.of(
                new EmaIndicator(),
                new SmaIndicator(),
                new RsiIndicator(),
                new MacdIndicator(),
                new StochasticIndicator()));

    // Matrix: EMA-3 on close (recursive) + SMA-2 on volume (windowed), daily only.

    IndicatorProperties properties = new IndicatorProperties();

    properties.setTimeframes(Map.of(Timeframe.DAILY, List.of(emaDef(), smaVolumeDef())));

    processor =
        new IndicatorTickerProcessor(
            registry, properties, dailyRepo, weeklyRepo, valueRepo, stateRepo);

    ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("TEST")
            .tickerName("Test")
            .isActive(true)
            .build();

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyBars());

    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    when(stateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void coldStartBackfillsWholeSeriesAndCheckpointsSecondToLastBar() {

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of());

    processor.processTicker(ticker);

    // Backfill deletes from the first bar for both combos.

    verify(valueRepo)
        .deleteCombo(
            ticker, Timeframe.DAILY, IndicatorType.EMA, PriceSource.CLOSE, "period=3", EPOCH);

    verify(valueRepo)
        .deleteCombo(
            ticker, Timeframe.DAILY, IndicatorType.SMA, PriceSource.VOLUME, "period=2", EPOCH);

    // EMA-3 over 12 bars is defined from index 2 -> 10 values.

    List<IndicatorValue> emaValues = capturedValues(IndicatorType.EMA);

    assertEquals(N - 2, emaValues.size());

    // Both combos checkpoint at the second-to-last bar; EMA carries internals, SMA does not.

    List<IndicatorState> states = capturedStates();

    IndicatorState emaState =
        states.stream()
            .filter(s -> s.getIndicatorType() == IndicatorType.EMA)
            .findFirst()
            .orElseThrow();

    IndicatorState smaState =
        states.stream()
            .filter(s -> s.getIndicatorType() == IndicatorType.SMA)
            .findFirst()
            .orElseThrow();

    assertEquals(date(N - 2), emaState.getLastPriceDate());

    assertNotNull(emaState.getInternals());

    assertEquals(date(N - 2), smaState.getLastPriceDate());

    assertNull(smaState.getInternals(), "windowed SMA carries no internals");
  }

  @Test
  void steadyStateResumeRewritesOnlyInProgressBar() {

    // Prior EMA checkpoint as of the second-to-last bar (index N-2), with correct internals.

    List<PriceBar> bars = toPriceBars(dailyBars());

    String internalsAtSecondToLast =
        new EmaIndicator()
            .compute(
                bars.subList(0, N - 1), null, IndicatorParams.parse("period=3"), PriceSource.CLOSE)
            .newStateJson();

    assertNotNull(internalsAtSecondToLast);

    IndicatorState priorEma =
        IndicatorState.builder()
            .ticker(ticker)
            .timeframe(Timeframe.DAILY)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params("period=3")
            .lastPriceDate(date(N - 2))
            .internals(internalsAtSecondToLast)
            .build();

    // Only the EMA combo here, to keep the assertions on the rewrite window unambiguous.

    IndicatorProperties properties = new IndicatorProperties();

    properties.setTimeframes(Map.of(Timeframe.DAILY, List.of(emaDef())));

    processor =
        new IndicatorTickerProcessor(
            new IndicatorRegistry(
                List.of(
                    new EmaIndicator(),
                    new SmaIndicator(),
                    new RsiIndicator(),
                    new MacdIndicator(),
                    new StochasticIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            valueRepo,
            stateRepo);

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of(priorEma));

    processor.processTicker(ticker);

    // Resume rewrites only from the in-progress last bar (index N-1).

    verify(valueRepo)
        .deleteCombo(
            ticker, Timeframe.DAILY, IndicatorType.EMA, PriceSource.CLOSE, "period=3", date(N - 1));

    List<IndicatorValue> written = capturedValues(IndicatorType.EMA);

    assertEquals(1, written.size());

    assertEquals(date(N - 1), written.getFirst().getPriceDate());

    // And the resumed value equals a full backfill at that date (bit-for-bit).

    BigDecimal fullValue =
        new EmaIndicator()
                .compute(bars, null, IndicatorParams.parse("period=3"), PriceSource.CLOSE)
                .values()
                .stream()
                .filter(p -> p.date().equals(date(N - 1)))
                .findFirst()
                .orElseThrow()
                .value();

    assertEquals(fullValue, written.getFirst().getValue());

    // Checkpoint does not regress (stays at the second-to-last bar).

    IndicatorState saved =
        capturedStates().stream()
            .filter(s -> s.getIndicatorType() == IndicatorType.EMA)
            .findFirst()
            .orElseThrow();

    assertEquals(date(N - 2), saved.getLastPriceDate());
  }

  @Test
  void warmupProducesNoCheckpointForRecursiveIndicator() {

    // Only 2 bars: EMA-3 cannot seed, so no value rows and no resumable checkpoint.

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyBars().subList(0, 2));

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of());

    processor.processTicker(ticker);

    // EMA: nothing to insert, and no state saved (still warming up).

    assertEquals(0, capturedValues(IndicatorType.EMA).size());

    boolean emaCheckpointed =
        capturedStates().stream().anyMatch(s -> s.getIndicatorType() == IndicatorType.EMA);

    org.junit.jupiter.api.Assertions.assertFalse(emaCheckpointed);
  }

  @Test
  void processTimeframeWithEmptyBarsReturnsEarly() {

    // Empty daily price bars should cause early return

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    processor.processTicker(ticker);

    verify(valueRepo, never()).deleteCombo(any(), any(), any(), any(), any(), any());
  }

  @Test
  void processTickerWithOneBarReturnsEarlyBeforeCheckpoint() {

    // n = 1 bar: can process values but cannot checkpoint (requires n >= 2)

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyBars().subList(0, 1));

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of());

    processor.processTicker(ticker);

    // verify states saving is skipped

    boolean stateSaved =
        capturedStates().stream().anyMatch(s -> s.getIndicatorType() == IndicatorType.EMA);

    assertFalse(stateSaved);
  }

  @Test
  void checkpointIsAtLastBarDoesNotResume() {

    // Prior checkpoint exists at the last bar (index N-1), so idx < n-1 is false.

    // Falls back to backfill/recompute or does not resume.

    IndicatorState priorEma =
        IndicatorState.builder()
            .ticker(ticker)
            .timeframe(Timeframe.DAILY)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params("period=3")
            .lastPriceDate(date(N - 1))
            .internals("{\"ema\":\"10.0\"}")
            .build();

    IndicatorProperties properties = new IndicatorProperties();

    properties.setTimeframes(Map.of(Timeframe.DAILY, List.of(emaDef())));

    processor =
        new IndicatorTickerProcessor(
            new IndicatorRegistry(
                List.of(
                    new EmaIndicator(),
                    new SmaIndicator(),
                    new RsiIndicator(),
                    new MacdIndicator(),
                    new StochasticIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            valueRepo,
            stateRepo);

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of(priorEma));

    processor.processTicker(ticker);

    // verify it deletes starting from the first bar (not resuming)

    verify(valueRepo)
        .deleteCombo(
            ticker, Timeframe.DAILY, IndicatorType.EMA, PriceSource.CLOSE, "period=3", EPOCH);
  }

  @Test
  void recursiveIndicatorWithNullInternalsDoesNotResume() {

    // Prior state exists but internals is null for recursive (EMA) indicator

    IndicatorState priorEma =
        IndicatorState.builder()
            .ticker(ticker)
            .timeframe(Timeframe.DAILY)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params("period=3")
            .lastPriceDate(date(N - 2))
            .internals(null)
            .build();

    IndicatorProperties properties = new IndicatorProperties();

    properties.setTimeframes(Map.of(Timeframe.DAILY, List.of(emaDef())));

    processor =
        new IndicatorTickerProcessor(
            new IndicatorRegistry(
                List.of(
                    new EmaIndicator(),
                    new SmaIndicator(),
                    new RsiIndicator(),
                    new MacdIndicator(),
                    new StochasticIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            valueRepo,
            stateRepo);

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of(priorEma));

    processor.processTicker(ticker);

    // Should NOT resume, so rewriteFrom should be the first bar's date (EPOCH)

    verify(valueRepo)
        .deleteCombo(
            ticker, Timeframe.DAILY, IndicatorType.EMA, PriceSource.CLOSE, "period=3", EPOCH);
  }

  @Test
  void windowedIndicatorResumesCorrectly() {

    // Prior state exists for SMA (windowed indicator) as of N-2

    IndicatorState priorSma =
        IndicatorState.builder()
            .ticker(ticker)
            .timeframe(Timeframe.DAILY)
            .indicatorType(IndicatorType.SMA)
            .source(PriceSource.VOLUME)
            .params("period=2")
            .lastPriceDate(date(N - 2))
            .internals(null)
            .build();

    IndicatorProperties properties = new IndicatorProperties();

    properties.setTimeframes(Map.of(Timeframe.DAILY, List.of(smaVolumeDef())));

    processor =
        new IndicatorTickerProcessor(
            new IndicatorRegistry(
                List.of(
                    new EmaIndicator(),
                    new SmaIndicator(),
                    new RsiIndicator(),
                    new MacdIndicator(),
                    new StochasticIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            valueRepo,
            stateRepo);

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of(priorSma));

    processor.processTicker(ticker);

    // Should resume! So rewriteFrom should be the last bar's date (date(N-1))

    verify(valueRepo)
        .deleteCombo(
            ticker,
            Timeframe.DAILY,
            IndicatorType.SMA,
            PriceSource.VOLUME,
            "period=2",
            date(N - 1));

    // Let's verify that the values captured are indeed only for N-1

    List<IndicatorValue> written = capturedValues(IndicatorType.SMA);

    assertEquals(1, written.size());

    assertEquals(date(N - 1), written.getFirst().getPriceDate());
  }

  @Test
  void weeklyTimeframeProcessing() {

    // Mock weekly prices returning a non-empty list to cover mapping

    com.alphaflow.persistence.entities.WeeklyPrice w1 =
        com.alphaflow.persistence.entities.WeeklyPrice.builder()
            .ticker(ticker)
            .priceDate(date(0))
            .priceOpen(BigDecimal.TEN)
            .priceHigh(BigDecimal.TEN)
            .priceLow(BigDecimal.TEN)
            .priceClose(BigDecimal.TEN)
            .volume(100L)
            .build();

    com.alphaflow.persistence.entities.WeeklyPrice w2 =
        com.alphaflow.persistence.entities.WeeklyPrice.builder()
            .ticker(ticker)
            .priceDate(date(1))
            .priceOpen(BigDecimal.TEN)
            .priceHigh(BigDecimal.TEN)
            .priceLow(BigDecimal.TEN)
            .priceClose(BigDecimal.TEN)
            .volume(100L)
            .build();

    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of(w1, w2));

    // Use weekly configuration

    IndicatorDefinition weeklyDef = new IndicatorDefinition();

    weeklyDef.setType(IndicatorType.EMA);

    weeklyDef.setSource(PriceSource.CLOSE);

    weeklyDef.setParams(Map.of("period", 2));

    IndicatorProperties properties = new IndicatorProperties();

    properties.setTimeframes(Map.of(Timeframe.WEEKLY, List.of(weeklyDef)));

    processor =
        new IndicatorTickerProcessor(
            new IndicatorRegistry(
                List.of(
                    new EmaIndicator(),
                    new SmaIndicator(),
                    new RsiIndicator(),
                    new MacdIndicator(),
                    new StochasticIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            valueRepo,
            stateRepo);

    processor.processTicker(ticker);

    // verify that delete/save was called for weekly timeframe

    verify(valueRepo)
        .deleteCombo(
            ticker, Timeframe.WEEKLY, IndicatorType.EMA, PriceSource.CLOSE, "period=2", date(0));
  }

  @Test
  void checkpointDateMissingDoesNotResume() {

    // Prior state exists but its lastPriceDate is NOT present in indexByDate

    IndicatorState priorEma =
        IndicatorState.builder()
            .ticker(ticker)
            .timeframe(Timeframe.DAILY)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params("period=3")
            .lastPriceDate(
                LocalDate.of(2000, 1, 1)) // Date that doesn't exist in our daily price bars
            .internals("{\"ema\":\"10.0\"}")
            .build();

    IndicatorProperties properties = new IndicatorProperties();

    properties.setTimeframes(Map.of(Timeframe.DAILY, List.of(emaDef())));

    processor =
        new IndicatorTickerProcessor(
            new IndicatorRegistry(
                List.of(
                    new EmaIndicator(),
                    new SmaIndicator(),
                    new RsiIndicator(),
                    new MacdIndicator(),
                    new StochasticIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            valueRepo,
            stateRepo);

    when(stateRepo.findByTickerAndTimeframe(ticker, Timeframe.DAILY)).thenReturn(List.of(priorEma));

    processor.processTicker(ticker);

    // Should NOT resume, so rewriteFrom should be the first bar's date (EPOCH)

    verify(valueRepo)
        .deleteCombo(
            ticker, Timeframe.DAILY, IndicatorType.EMA, PriceSource.CLOSE, "period=3", EPOCH);
  }

  @SuppressWarnings("unchecked")
  private List<IndicatorValue> capturedValues(IndicatorType type) {

    ArgumentCaptor<List<IndicatorValue>> captor = ArgumentCaptor.forClass(List.class);

    verify(valueRepo, org.mockito.Mockito.atLeast(0)).saveAll(captor.capture());

    return captor.getAllValues().stream()
        .flatMap(List::stream)
        .filter(v -> v.getIndicatorType() == type)
        .toList();
  }

  private List<IndicatorState> capturedStates() {

    ArgumentCaptor<IndicatorState> captor = ArgumentCaptor.forClass(IndicatorState.class);

    verify(stateRepo, org.mockito.Mockito.atLeast(0)).save(captor.capture());

    return captor.getAllValues();
  }
}
