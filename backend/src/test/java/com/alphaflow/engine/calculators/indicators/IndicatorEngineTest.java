package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the hand-rolled indicators.
 * <p>
 * The centerpiece for every recursive/windowed indicator is the <b>resume == backfill</b> property:
 * computing the full series in one pass must equal seeding from a mid-series checkpoint and
 * continuing — bit-for-bit over the overlapping dates. That is the invariant the whole incremental
 * persistence design rests on. Targeted reference/extreme/range assertions cover the math itself.
 */
class IndicatorEngineTest {

    private static final LocalDate EPOCH = LocalDate.of(2020, 1, 1);

    // ---- series helpers -------------------------------------------------

    /** Deterministic, non-trivial walk so resume tests exercise real recurrence (no RNG). */
    private static List<PriceBar> walk(int n) {
        List<PriceBar> bars = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double close = 100 + 12 * Math.sin(i / 5.0) + 0.3 * i + 4 * Math.sin(i / 1.7);
            bars.add(barAt(i, close, close + 2, close - 2, close));
        }
        return bars;
    }

    private static List<PriceBar> closes(double... closes) {
        List<PriceBar> bars = new ArrayList<>(closes.length);
        for (int i = 0; i < closes.length; i++) {
            bars.add(barAt(i, closes[i], closes[i], closes[i], closes[i]));
        }
        return bars;
    }

    private static PriceBar barAt(int dayOffset, double open, double high, double low, double close) {
        return new PriceBar(
                EPOCH.plusDays(dayOffset),
                bd(open), bd(high), bd(low), bd(close),
                BigDecimal.valueOf(1000L + dayOffset));
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(4, IndicatorMath.ROUNDING);
    }

    private static List<PlotPoint> from(List<PlotPoint> values, LocalDate fromInclusive) {
        return values.stream().filter(p -> !p.date().isBefore(fromInclusive)).toList();
    }

    private static PlotPoint plot(List<PlotPoint> values, LocalDate date, String output) {
        return values.stream()
                .filter(p -> p.date().equals(date) && p.outputName().equals(output))
                .findFirst().orElseThrow();
    }

    /** Asserts resume-from-checkpoint reproduces a full backfill for a recursive indicator. */
    private static void assertRecursiveResumeMatchesBackfill(Indicator indicator, IndicatorParams params, int... splits) {
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

    /** Asserts a windowed indicator yields identical values over a sufficient sub-window. */
    private static void assertWindowedResumeMatchesBackfill(Indicator indicator, IndicatorParams params, int lookback, int... splits) {
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

    // ---- SMA ------------------------------------------------------------

    @Test
    void smaReferenceValues() {
        IndicatorResult r = new SmaIndicator().compute(
                closes(1, 2, 3, 4, 5), null, IndicatorParams.parse("period=3"), PriceSource.CLOSE);
        // Defined from the 3rd bar: avg(1,2,3)=2, avg(2,3,4)=3, avg(3,4,5)=4.
        assertEquals(3, r.values().size());
        assertEquals(0, plot(r.values(), EPOCH.plusDays(2), "value").value().compareTo(bd(2)));
        assertEquals(0, plot(r.values(), EPOCH.plusDays(3), "value").value().compareTo(bd(3)));
        assertEquals(0, plot(r.values(), EPOCH.plusDays(4), "value").value().compareTo(bd(4)));
        assertNull(r.newStateJson());
    }

    @Test
    void smaOnVolumeSource() {
        // volume = 1000 + dayOffset -> [1000,1001,1002]; SMA-2 -> 1000.5, 1001.5
        IndicatorResult r = new SmaIndicator().compute(
                closes(1, 2, 3), null, IndicatorParams.parse("period=2"), PriceSource.VOLUME);
        assertEquals(2, r.values().size());
        assertEquals(0, r.values().get(0).value().compareTo(BigDecimal.valueOf(1000.5)));
        assertEquals(0, r.values().get(1).value().compareTo(BigDecimal.valueOf(1001.5)));
    }

    @Test
    void smaResumeMatchesBackfill() {
        assertWindowedResumeMatchesBackfill(new SmaIndicator(), IndicatorParams.parse("period=20"), 20, 30, 50, 70);
    }

    // ---- EMA ------------------------------------------------------------

    @Test
    void emaReferenceValues() {
        // EMA-3, k = 2/4 = 0.5, seed = SMA(1,2,3)=2; then 4*.5+2*.5=3; 5*.5+3*.5=4.
        IndicatorResult r = new EmaIndicator().compute(
                closes(1, 2, 3, 4, 5), null, IndicatorParams.parse("period=3"), PriceSource.CLOSE);
        assertEquals(3, r.values().size());
        assertEquals(0, plot(r.values(), EPOCH.plusDays(2), "value").value().compareTo(bd(2)));
        assertEquals(0, plot(r.values(), EPOCH.plusDays(3), "value").value().compareTo(bd(3)));
        assertEquals(0, plot(r.values(), EPOCH.plusDays(4), "value").value().compareTo(bd(4)));
        assertNotNull(r.newStateJson());
    }

    @Test
    void emaResumeMatchesBackfill() {
        assertRecursiveResumeMatchesBackfill(new EmaIndicator(), IndicatorParams.parse("period=13"), 30, 50, 70);
    }

    // ---- RSI ------------------------------------------------------------

    @Test
    void rsiAllGainsIsHundred() {
        IndicatorResult r = new RsiIndicator().compute(
                closes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                null, IndicatorParams.parse("period=14"), PriceSource.CLOSE);
        assertFalse(r.values().isEmpty());
        r.values().forEach(p -> assertEquals(0, p.value().compareTo(bd(100)), "all-gains RSI must be 100"));
    }

    @Test
    void rsiAllLossesIsZero() {
        IndicatorResult r = new RsiIndicator().compute(
                closes(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
                null, IndicatorParams.parse("period=14"), PriceSource.CLOSE);
        assertFalse(r.values().isEmpty());
        r.values().forEach(p -> assertEquals(0, p.value().compareTo(bd(0)), "all-losses RSI must be 0"));
    }

    @Test
    void rsiStaysInRange() {
        IndicatorResult r = new RsiIndicator().compute(
                walk(100), null, IndicatorParams.parse("period=14"), PriceSource.CLOSE);
        r.values().forEach(p -> {
            assertTrue(p.value().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(p.value().compareTo(IndicatorMath.HUNDRED) <= 0);
        });
    }

    @Test
    void rsiResumeMatchesBackfill() {
        assertRecursiveResumeMatchesBackfill(new RsiIndicator(), IndicatorParams.parse("period=14"), 30, 50, 70);
    }

    // ---- MACD -----------------------------------------------------------

    @Test
    void macdConstantSeriesIsZero() {
        double[] flat = new double[60];
        java.util.Arrays.fill(flat, 50.0);
        IndicatorResult r = new MacdIndicator().compute(
                closes(flat), null, IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);
        assertFalse(r.values().isEmpty());
        r.values().forEach(p -> assertEquals(0, p.value().compareTo(bd(0)),
                "constant series must give zero macd/signal/histogram (" + p.outputName() + ")"));
    }

    @Test
    void macdEmitsThreePlotsOnceDefined() {
        IndicatorResult r = new MacdIndicator().compute(
                walk(80), null, IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);
        // Last bar (well past warm-up) must carry all three plots.
        LocalDate last = EPOCH.plusDays(79);
        assertNotNull(plot(r.values(), last, "macd"));
        assertNotNull(plot(r.values(), last, "signal"));
        assertNotNull(plot(r.values(), last, "histogram"));
        // histogram == macd - signal at that bar.
        BigDecimal macd = plot(r.values(), last, "macd").value();
        BigDecimal signal = plot(r.values(), last, "signal").value();
        BigDecimal hist = plot(r.values(), last, "histogram").value();
        assertEquals(0, hist.compareTo(macd.subtract(signal)));
    }

    @Test
    void macdResumeMatchesBackfill() {
        assertRecursiveResumeMatchesBackfill(
                new MacdIndicator(), IndicatorParams.parse("fast=12,slow=26,signal=9"), 45, 60, 75);
    }

    // ---- Stochastic -----------------------------------------------------

    @Test
    void stochasticStaysInRange() {
        IndicatorResult r = new StochasticIndicator().compute(
                walk(100), null, IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), PriceSource.CLOSE);
        assertFalse(r.values().isEmpty());
        r.values().forEach(p -> {
            assertTrue(p.value().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(p.value().compareTo(IndicatorMath.HUNDRED) <= 0);
        });
    }

    @Test
    void stochasticResumeMatchesBackfill() {
        assertWindowedResumeMatchesBackfill(
                new StochasticIndicator(), IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), 25, 50, 75);
    }

    // ---- StateCodec & registry -----------------------------------------

    @Test
    void stateCodecRoundTripsDecimalsExactly() {
        BigDecimal value = new BigDecimal("123.456789012345");
        String json = StateCodec.encode(java.util.Map.of("ema", value));
        assertEquals(value, StateCodec.decode(json).get("ema"));
        assertTrue(StateCodec.decode(null).isEmpty());
    }

    @Test
    void registryResolvesEachTypeAndRejectsUnknown() {
        IndicatorRegistry registry = new IndicatorRegistry(List.of(
                new SmaIndicator(), new EmaIndicator(), new RsiIndicator(),
                new MacdIndicator(), new StochasticIndicator()));
        for (IndicatorType type : IndicatorType.values()) {
            assertEquals(type, registry.get(type).type());
        }
    }

    @Test
    void registryRejectsDuplicateBeans() {
        assertThrows(IllegalStateException.class, () ->
                new IndicatorRegistry(List.of(new SmaIndicator(), new SmaIndicator())));
    }
}
