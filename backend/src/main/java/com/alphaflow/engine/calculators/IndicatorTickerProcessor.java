package com.alphaflow.engine.calculators;

import com.alphaflow.engine.calculators.indicators.Indicator;
import com.alphaflow.engine.calculators.indicators.IndicatorParams;
import com.alphaflow.engine.calculators.indicators.IndicatorRegistry;
import com.alphaflow.engine.calculators.indicators.PlotPoint;
import com.alphaflow.engine.calculators.indicators.PriceBar;
import com.alphaflow.engine.configs.IndicatorProperties;
import com.alphaflow.engine.configs.IndicatorProperties.IndicatorDefinition;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes and persists every configured indicator for a single ticker, across both timeframes.
 * <p>
 * Lives in its own bean (not as a method on {@link IndicatorCalculator}) so the {@link Transactional}
 * boundary is honored — a self-invocation would bypass Spring's proxy. The whole ticker is one
 * transaction: its published values and checkpoints commit atomically, so they can never diverge.
 *
 * <h4>Per-combo algorithm</h4>
 * For each (timeframe, indicator, source, params):
 * <ol>
 *   <li><b>Resume vs. backfill.</b> If a checkpoint exists, its {@code lastPriceDate} is present in
 *       the bars, and it is resumable (recursive with internals, or windowed), resume from the bar
 *       after it; otherwise (cold start, new indicator, or a contiguity gap) recompute from history.</li>
 *   <li><b>Publish values</b> for the recompute window (resume: bars after the checkpoint, including
 *       the in-progress last bar; backfill: the whole series) by deleting that date range and
 *       reinserting the freshly computed plots.</li>
 *   <li><b>Checkpoint</b> the running state as of the <i>second-to-last</i> bar — never the most
 *       recent one — so the in-progress bar (e.g. the current week) is republished every run but
 *       never frozen into resume state.</li>
 * </ol>
 * Note: this loads each ticker's full price history per run (a read); the write volume is what is
 * minimized (only the tail is rewritten). Tail-only <i>loading</i> is a deferred optimization.
 */
@Component
public class IndicatorTickerProcessor {

    private static final Logger log = LoggerFactory.getLogger(IndicatorTickerProcessor.class);

    private final IndicatorRegistry registry;
    private final IndicatorProperties properties;
    private final DailyPriceRepository dailyPriceRepository;
    private final WeeklyPriceRepository weeklyPriceRepository;
    private final IndicatorValueRepository indicatorValueRepository;
    private final IndicatorStateRepository indicatorStateRepository;

    public IndicatorTickerProcessor(
            IndicatorRegistry registry,
            IndicatorProperties properties,
            DailyPriceRepository dailyPriceRepository,
            WeeklyPriceRepository weeklyPriceRepository,
            IndicatorValueRepository indicatorValueRepository,
            IndicatorStateRepository indicatorStateRepository
    ) {
        this.registry = registry;
        this.properties = properties;
        this.dailyPriceRepository = dailyPriceRepository;
        this.weeklyPriceRepository = weeklyPriceRepository;
        this.indicatorValueRepository = indicatorValueRepository;
        this.indicatorStateRepository = indicatorStateRepository;
    }

    @Transactional
    public void processTicker(Ticker ticker) {
        processTimeframe(ticker, Timeframe.DAILY, loadDailyBars(ticker));
        processTimeframe(ticker, Timeframe.WEEKLY, loadWeeklyBars(ticker));
    }

    private void processTimeframe(Ticker ticker, Timeframe timeframe, List<PriceBar> bars) {
        List<IndicatorDefinition> definitions = properties.forTimeframe(timeframe);
        if (definitions.isEmpty() || bars.isEmpty()) {
            return;
        }

        Map<LocalDate, Integer> indexByDate = new HashMap<>();
        for (int i = 0; i < bars.size(); i++) {
            indexByDate.put(bars.get(i).date(), i);
        }

        Map<String, IndicatorState> stateByCombo = new HashMap<>();
        for (IndicatorState state : indicatorStateRepository.findByTickerAndTimeframe(ticker, timeframe)) {
            stateByCombo.put(comboKey(state.getIndicatorType(), state.getSource(), state.getParams()), state);
        }

        for (IndicatorDefinition definition : definitions) {
            processCombo(ticker, timeframe, bars, indexByDate, stateByCombo, definition);
        }
    }

    private void processCombo(Ticker ticker,
                              Timeframe timeframe,
                              List<PriceBar> bars,
                              Map<LocalDate, Integer> indexByDate,
                              Map<String, IndicatorState> stateByCombo,
                              IndicatorDefinition definition) {
        Indicator indicator = registry.get(definition.getType());
        IndicatorParams params = IndicatorParams.of(definition.getParams());
        String paramsCanonical = params.canonical();
        PriceSource source = definition.getSource();
        IndicatorType type = definition.getType();
        boolean windowed = !indicator.requiresState();
        int n = bars.size();

        IndicatorState prior = stateByCombo.get(comboKey(type, source, paramsCanonical));

        // 1. Decide resume vs. backfill (with contiguity check).
        boolean resume = false;
        int checkpointIndex = -1;
        String seedJson = null;
        if (prior != null && (windowed || prior.getInternals() != null)) {
            Integer idx = indexByDate.get(prior.getLastPriceDate());
            if (idx != null && idx < n - 1) {
                resume = true;
                checkpointIndex = idx;
                seedJson = windowed ? null : prior.getInternals();
            }
            // idx == null (checkpoint date missing -> data revision/gap) or idx >= n-1 (no new bar):
            // fall back to a full recompute, which self-heals.
        }

        // 2. Compute the publishable values and the date from which to rewrite them.
        List<PlotPoint> computed;
        LocalDate rewriteFrom;
        if (resume) {
            // Windowed indicators need their full lookback, so recompute over all bars and filter;
            // recursive indicators resume from the persisted state over just the new tail.
            computed = windowed
                    ? indicator.compute(bars, null, params, source).values()
                    : indicator.compute(bars.subList(checkpointIndex + 1, n), seedJson, params, source).values();
            rewriteFrom = bars.get(checkpointIndex + 1).date();
        } else {
            computed = indicator.compute(bars, null, params, source).values();
            rewriteFrom = bars.getFirst().date();
        }

        // 3. Replace published values for the rewrite window (bulk delete runs before inserts flush).
        indicatorValueRepository.deleteCombo(ticker, timeframe, type, source, paramsCanonical, rewriteFrom);
        List<IndicatorValue> toInsert = new ArrayList<>();
        for (PlotPoint point : computed) {
            if (point.date().isBefore(rewriteFrom)) {
                continue;
            }
            toInsert.add(IndicatorValue.builder()
                    .ticker(ticker)
                    .timeframe(timeframe)
                    .indicatorType(type)
                    .source(source)
                    .params(paramsCanonical)
                    .outputName(point.outputName())
                    .priceDate(point.date())
                    .value(point.value())
                    .build());
        }
        if (!toInsert.isEmpty()) {
            indicatorValueRepository.saveAll(toInsert);
        }

        // 4. Checkpoint the state as of the second-to-last bar (never the in-progress last bar).
        if (n < 2) {
            return; // no finalized bar to checkpoint yet
        }
        String checkpointInternals;
        if (windowed) {
            checkpointInternals = null;
        } else if (resume) {
            // bars.subList(ci+1, n-1) excludes the in-progress bar; empty when ci == n-2 (state unchanged).
            checkpointInternals = indicator.compute(bars.subList(checkpointIndex + 1, n - 1), seedJson, params, source)
                    .newStateJson();
        } else {
            checkpointInternals = indicator.compute(bars.subList(0, n - 1), null, params, source).newStateJson();
        }

        boolean checkpointable = windowed || checkpointInternals != null;
        if (!checkpointable) {
            return; // recursive indicator still warming up: no resumable state yet
        }

        IndicatorState state = prior != null ? prior : IndicatorState.builder()
                .ticker(ticker)
                .timeframe(timeframe)
                .indicatorType(type)
                .source(source)
                .params(paramsCanonical)
                .build();
        state.setLastPriceDate(bars.get(n - 2).date());
        state.setInternals(checkpointInternals);
        IndicatorState saved = indicatorStateRepository.save(state);
        stateByCombo.put(comboKey(type, source, paramsCanonical), saved);
    }

    private static String comboKey(IndicatorType type, PriceSource source, String params) {
        return type + "|" + source + "|" + params;
    }

    private List<PriceBar> loadDailyBars(Ticker ticker) {
        return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
                .map(d -> new PriceBar(d.getPriceDate(), d.getPriceOpen(), d.getPriceHigh(),
                        d.getPriceLow(), d.getPriceClose(), BigDecimal.valueOf(d.getVolume())))
                .toList();
    }

    private List<PriceBar> loadWeeklyBars(Ticker ticker) {
        return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
                .map(w -> new PriceBar(w.getPriceDate(), w.getPriceOpen(), w.getPriceHigh(),
                        w.getPriceLow(), w.getPriceClose(), BigDecimal.valueOf(w.getVolume())))
                .toList();
    }
}
