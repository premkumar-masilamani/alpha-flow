package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.Bucket;
import com.alphaflow.engine.calculators.enums.LevelType;
import com.alphaflow.engine.configs.SupportResistanceConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.engine.structure.DowAtrSwingDetector;
import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.SwingType;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes and persists horizontal support and resistance (S&amp;R) zones across daily and weekly
 * timeframes using a four-stage Dow-ATR pipeline.
 *
 * <p>Pipeline stages:
 *
 * <ol>
 *   <li>Compute Wilder's rolling ATR series once for the entire price history.
 *   <li>Detect Close-confirmed Dow Theory swing pivots via {@link DowAtrSwingDetector}.
 *   <li>Cluster nearby swing pivots into pivot-anchored candidate bands sized to current-regime
 *       volatility and validate each candidate chronologically for breaches, polarity flips, and
 *       false breakouts.
 *   <li>Deduplicate overlapping surviving candidates via greedy non-overlapping suppression ranked
 *       by touch strength and recency.
 * </ol>
 */
@Component
@Slf4j
public class SupportResistanceCalculator implements PipelineJob {

  public static final String JOB_NAME = "CALCULATE_SUPPORT_RESISTANCE";

  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailySupportResistanceRepository dailySupportResistanceRepository;
  private final WeeklySupportResistanceRepository weeklySupportResistanceRepository;
  private final DowAtrSwingDetector dowAtrSwingDetector;

  @Autowired @Lazy private SupportResistanceCalculator self;

  public SupportResistanceCalculator(
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository,
      DowAtrSwingDetector dowAtrSwingDetector) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
    this.dowAtrSwingDetector = dowAtrSwingDetector;
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    List<Object[]> syncStates = loadSyncStates(timeframe);
    LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
    for (Object[] row : syncStates) {
      LocalDate sourceDate = (LocalDate) row[1];
      LocalDate calcDate = (LocalDate) row[2];
      if (PipelineGating.requiresUpdate(timeframe, sourceDate, calcDate, todayUtc)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void execute(Timeframe timeframe) {
    computeSupportResistances(timeframe);
  }

  /**
   * Computes S&amp;R zones for all stale active tickers on the requested timeframe using a single
   * upfront bulk state query, isolating failures so an error on one ticker does not abort the
   * remaining tickers.
   *
   * @param timeframe market timeframe to compute
   */
  public void computeSupportResistances(Timeframe timeframe) {
    log.info("Computing {} support and resistances...", timeframe);
    List<Object[]> syncStates = loadSyncStates(timeframe);
    LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
    List<Object[]> staleStates = new ArrayList<>();
    for (Object[] row : syncStates) {
      Ticker ticker = (Ticker) row[0];
      LocalDate sourceDate = (LocalDate) row[1];
      LocalDate calcDate = (LocalDate) row[2];
      if (PipelineGating.requiresUpdate(timeframe, sourceDate, calcDate, todayUtc)) {
        staleStates.add(row);
      } else {
        log.debug(
            "{}: up to date (source={}, calculated={}). Skipping.",
            ticker.getTickerSymbol(),
            sourceDate,
            calcDate);
      }
    }

    int totalActiveTickers = syncStates.size();
    int staleCount = staleStates.size();
    if (staleCount > 0) {
      log.info(
          "{} S&R computation: {} of {} active tickers require processing (skipping {} up-to-date tickers).",
          timeframe,
          staleCount,
          totalActiveTickers,
          totalActiveTickers - staleCount);

      // Invoke via Spring proxy so each ticker runs in its own REQUIRES_NEW transaction boundary.
      SupportResistanceCalculator proxy = (self != null) ? self : this;

      for (Object[] row : staleStates) {
        Ticker ticker = (Ticker) row[0];
        LocalDate calcDate = (LocalDate) row[2];
        try {
          proxy.computeSupportResistancesForTicker(
              ticker, timeframe, calcDate);
        } catch (Exception exception) {
          log.error(
              "Failed to compute {} S&R for ticker {}: {}",
              timeframe,
              ticker.getTickerSymbol(),
              exception.getMessage(),
              exception);
        }
      }
      log.info("{} support and resistances computed.", timeframe);
    } else {
      log.info(
          "{} S&R computation: 0 of {} active tickers require processing; skipping step.",
          timeframe,
          totalActiveTickers);
    }
  }

  /**
   * Computes and persists the latest S&amp;R snapshot for a single ticker within an isolated
   * transaction. Replaces existing weekly snapshot rows idempotently during mid-week refreshes and
   * purges expired snapshots beyond the configured timeframe retention window.
   *
   * @param ticker target ticker entity
   * @param timeframe target market timeframe
   * @param latestCalculatedDate latest persisted snapshot date from the bulk sync query (nullable)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeSupportResistancesForTicker(
      Ticker ticker, Timeframe timeframe, LocalDate latestCalculatedDate) {
    log.debug("Computing S&R for ticker {} and timeframe {}", ticker.getTickerSymbol(), timeframe);
    List<PriceBar> priceBars = loadBars(ticker, timeframe);
    if (!priceBars.isEmpty()) {
      LocalDate priceDate = priceBars.getLast().date();
      LocalDate cutoffDate =
          priceDate.minusDays(SupportResistanceConfig.getRetentionDaysForTimeframe(timeframe));
      log.debug(
          "Purging {} S&R records older than {} for ticker {}.",
          timeframe,
          cutoffDate,
          ticker.getTickerSymbol());
      deleteOlderSupportResistances(ticker, cutoffDate, timeframe);

      if (timeframe == Timeframe.WEEKLY
          && latestCalculatedDate != null
          && !latestCalculatedDate.isBefore(priceDate)) {
        log.debug(
            "Refreshing in-progress weekly S&R zones for ticker {} on {}.",
            ticker.getTickerSymbol(),
            priceDate);
        weeklySupportResistanceRepository.deleteByTickerAndPriceDate(ticker, priceDate);
      }

      List<Bucket> buckets = computeBuckets(priceBars);

      if (!buckets.isEmpty()) {
        saveSupportResistances(ticker, buckets, timeframe);
        log.info(
            "Saved {} {} S&R zones for ticker {} (priceDate={}).",
            buckets.size(),
            timeframe,
            ticker.getTickerSymbol(),
            priceDate);
      } else {
        log.debug(
            "No surviving {} S&R zones computed for ticker {} (priceDate={}).",
            timeframe,
            ticker.getTickerSymbol(),
            priceDate);
      }
    } else {
      log.debug(
          "No {} price bars found for ticker {}; skipping S&R computation.",
          timeframe,
          ticker.getTickerSymbol());
    }
  }

  /**
   * Runs the pure in-memory S&amp;R computation pipeline for a chronologically ordered bar series.
   *
   * <p>Wilder ATR is computed once and reused across all downstream stages: historical per-bar ATR
   * drives swing reversal detection and lifecycle breach checks, while the latest ATR sizes the
   * actionable thickness of today's zones.
   *
   * @param bars chronologically ordered OHLCV bars
   * @return surviving non-overlapping S&amp;R zones ordered from highest midpoint to lowest
   */
  public List<Bucket> computeBuckets(List<PriceBar> bars) {
    // Stage 1: Compute Wilder ATR once for reuse across swing detection, sizing, and breach checks.
    List<BigDecimal> atrSeries =
        IndicatorMath.calculateWilderAtrSeries(bars, SupportResistanceConfig.ATR_PERIOD);

    // Stage 2: Extract alternating Close-confirmed Dow Theory swing pivots.
    List<SwingPoint> swings =
        dowAtrSwingDetector.detectSwings(
            bars,
            atrSeries,
            SupportResistanceConfig.ATR_PERIOD,
            SupportResistanceConfig.SWING_ATR_MULTIPLIER);

    if (swings.size() >= SupportResistanceConfig.MIN_TOUCH_COUNT) {
      // Size current-chart zones using latest volatility so all candidates share one uniform ruler.
      BigDecimal latestAtr = atrSeries.getLast();
      BigDecimal zoneWidth = latestAtr.multiply(SupportResistanceConfig.ZONE_ATR_MULTIPLIER);

      // Stage 3: Cluster pivots into candidate bands and replay historical lifecycle rules.
      List<Bucket> candidates = buildCandidateZones(bars, atrSeries, swings, zoneWidth);

      // Stage 4: Keep only the strongest non-overlapping zones.
      List<Bucket> selected = applyNonOverlappingSuppression(candidates);
      log.debug(
          "S&R zone computation completed: {} swings -> {} surviving candidates -> {} final zones "
              + "(latestAtr={}, zoneWidth={}).",
          swings.size(),
          candidates.size(),
          selected.size(),
          IndicatorMath.publish(latestAtr),
          IndicatorMath.publish(zoneWidth));
      return selected;
    } else {
      log.debug(
          "Insufficient confirmed Dow swings ({} < {}) to form S&R zones.",
          swings.size(),
          SupportResistanceConfig.MIN_TOUCH_COUNT);
      return List.of();
    }
  }

  /**
   * Deduplicates overlapping candidate zones using greedy non-overlapping suppression. Candidates
   * are prioritized by highest surviving touch count, then most recent touch date, then highest
   * midpoint; any lower-ranked candidate whose price interval strictly overlaps an already selected
   * zone is discarded.
   *
   * @param candidates surviving candidate zones from lifecycle evaluation
   * @return non-overlapping zones sorted by midpoint descending
   */
  public List<Bucket> applyNonOverlappingSuppression(List<Bucket> candidates) {
    List<Bucket> sorted =
        candidates.stream()
            .sorted(
                Comparator.comparingInt(Bucket::getTouchCount)
                    .reversed()
                    .thenComparing(Bucket::getLastTouchDate, Comparator.reverseOrder())
                    .thenComparing(Bucket::getMidpoint, Comparator.reverseOrder()))
            .toList();

    List<Bucket> selected = new ArrayList<>();
    for (Bucket candidate : sorted) {
      boolean overlaps = false;
      for (Bucket kept : selected) {
        // Strict open-interval overlap check: [bottom, top] overlaps iff both ranges intersect.
        if (candidate.getBottom().compareTo(kept.getTop()) < 0
            && candidate.getTop().compareTo(kept.getBottom()) > 0) {
          overlaps = true;
          break;
        }
      }
      if (!overlaps) {
        selected.add(candidate);
      }
    }
    // Present final zones in descending price order for consistent persistence and display.
    selected.sort(Comparator.comparing(Bucket::getMidpoint).reversed());
    return selected;
  }

  /**
   * Generates and validates candidate S&amp;R zones via price-sorted two-pointer clustering.
   *
   * <p>Swings are sorted by price ascending so search windows move monotonically, advancing sliding
   * pointers {@code left} and {@code right} in O(S) time across the entire pass. Consecutive
   * anchors capturing identical member subsets are deduplicated in O(1) time without Set overhead.
   * Clusters meeting {@code MIN_TOUCH_COUNT} are centered on their member price mean to establish
   * stationary breach boundaries and passed to {@link #evaluateZoneLifecycle}.
   */
  private List<Bucket> buildCandidateZones(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      List<SwingPoint> swings,
      BigDecimal zoneWidth) {

    BigDecimal halfZone = IndicatorMath.divide(zoneWidth, BigDecimal.valueOf(2));

    List<SwingPoint> sortedByPrice =
        swings.stream().sorted(Comparator.comparing(SwingPoint::price)).toList();
    int swingCount = sortedByPrice.size();

    List<Bucket> candidates = new ArrayList<>();
    int left = 0;
    int right = 0;
    int prevLeft = -1;
    int prevRight = -1;

    for (int i = 0; i < swingCount; i++) {
      SwingPoint anchor = sortedByPrice.get(i);
      BigDecimal windowBottom = anchor.price().subtract(halfZone);
      BigDecimal windowTop = anchor.price().add(halfZone);

      while (right < swingCount && sortedByPrice.get(right).price().compareTo(windowTop) <= 0) {
        right++;
      }
      while (sortedByPrice.get(left).price().compareTo(windowBottom) < 0) {
        left++;
      }

      int memberCount = right - left;
      if (memberCount >= SupportResistanceConfig.MIN_TOUCH_COUNT) {
        if (left != prevLeft || right != prevRight) {
          prevLeft = left;
          prevRight = right;

          List<SwingPoint> members = sortedByPrice.subList(left, right);
          BigDecimal sumPrice = BigDecimal.ZERO;
          for (SwingPoint member : members) {
            sumPrice = sumPrice.add(member.price());
          }

          BigDecimal initialCentroid =
              IndicatorMath.divide(sumPrice, BigDecimal.valueOf(memberCount));
          BigDecimal initialBottom = IndicatorMath.publish(initialCentroid.subtract(halfZone));
          BigDecimal initialTop = IndicatorMath.publish(initialCentroid.add(halfZone));

          Bucket survivingZone =
              evaluateZoneLifecycle(bars, atrSeries, members, initialBottom, initialTop, halfZone);
          if (survivingZone != null) {
            candidates.add(survivingZone);
          }
        }
      }
    }
    return candidates;
  }

  /**
   * Replays price history chronologically from a cluster's earliest swing to the latest bar to
   * validate structural integrity under volatility-confirmed breach rules.
   *
   * <p>For each bar after the zone activates:
   *
   * <ul>
   *   <li>A Close beyond the boundary plus {@code BREACH_ATR_MULTIPLIER * ATR[i]} is a decisive
   *       breach: it flips the zone's active polarity up to {@code MAX_ALLOWED_FLIPS} times (and
   *       resets false-breakout tracking), after which any further decisive breach invalidates and
   *       clears accumulated touches.
   *   <li>A Close outside the boundary without exceeding the decisive margin marks a mild breach;
   *       reclaiming the safe side increments {@code falseBreakoutCount}, and exceeding {@code
   *       MAX_FALSE_BREAKOUTS} invalidates and clears accumulated touches.
   * </ul>
   *
   * <p>If at least {@code MIN_TOUCH_COUNT} active touches survive through the latest bar, the final
   * zone band is rebuilt around the surviving touches' centroid and classified relative to the
   * latest bar's Close.
   *
   * @return validated {@link Bucket}, or {@code null} if fewer than {@code MIN_TOUCH_COUNT} touches
   *     survive
   */
  private Bucket evaluateZoneLifecycle(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      List<SwingPoint> members,
      BigDecimal initialZoneBottom,
      BigDecimal initialZoneTop,
      BigDecimal halfZone) {
    // Index clustered swing pivots by bar index and resolve earliest activation bar.
    Map<Integer, SwingPoint> membersByBarIndex = new HashMap<>(members.size());
    int earliestBarIndex = Integer.MAX_VALUE;
    for (SwingPoint member : members) {
      membersByBarIndex.put(member.barIndex(), member);
      if (member.barIndex() < earliestBarIndex) {
        earliestBarIndex = member.barIndex();
      }
    }

    List<SwingPoint> activeMembers = new ArrayList<>();
    LevelType activeRole = null;
    int flipCount = 0;
    int falseBreakoutCount = 0;
    boolean inMildBreach = false;

    for (int i = earliestBarIndex; i < bars.size(); i++) {
      // Activate or reinforce the zone whenever the replay reaches one of its clustered pivots.
      SwingPoint swingAtBar = membersByBarIndex.get(i);
      if (swingAtBar != null) {
        if (activeMembers.isEmpty()) {
          // Establish initial structural role from the first pivot after creation or reset.
          activeRole =
              (swingAtBar.type() == SwingType.LOW) ? LevelType.SUPPORT : LevelType.RESISTANCE;
        }
        activeMembers.add(swingAtBar);
      }

      if (!activeMembers.isEmpty()) {
        BigDecimal barClose = bars.get(i).close();
        // Scale breach confirmation to bar i's contemporaneous ATR.
        BigDecimal breachMargin =
            atrSeries.get(i).multiply(SupportResistanceConfig.BREACH_ATR_MULTIPLIER);
        boolean isDecisiveBreach;
        boolean isMildBreach;

        if (activeRole == LevelType.SUPPORT) {
          isDecisiveBreach = barClose.compareTo(initialZoneBottom.subtract(breachMargin)) < 0;
          isMildBreach = barClose.compareTo(initialZoneBottom) < 0;
        } else {
          isDecisiveBreach = barClose.compareTo(initialZoneTop.add(breachMargin)) > 0;
          isMildBreach = barClose.compareTo(initialZoneTop) > 0;
        }

        if (isDecisiveBreach) {
          if (flipCount < SupportResistanceConfig.MAX_ALLOWED_FLIPS) {
            // Clean polarity reversal: flip active role and reset mild-breach counters.
            flipCount++;
            activeRole =
                (activeRole == LevelType.SUPPORT) ? LevelType.RESISTANCE : LevelType.SUPPORT;
            inMildBreach = false;
            falseBreakoutCount = 0;
          } else {
            // Zone exceeded allowed flips: discard prior touch history so it must rebuild fresh.
            activeMembers.clear();
            activeRole = null;
            flipCount = 0;
            falseBreakoutCount = 0;
            inMildBreach = false;
          }
        } else if (isMildBreach) {
          // Price closed past the zone edge without decisive conviction; await reclaim or break.
          inMildBreach = true;
        } else if (inMildBreach) {
          // Price reclaimed the safe side after a mild breach; count one completed false breakout.
          falseBreakoutCount++;
          inMildBreach = false;
          if (falseBreakoutCount > SupportResistanceConfig.MAX_FALSE_BREAKOUTS) {
            // Excessive whipsaw invalidates prior touches and resets lifecycle tracking.
            activeMembers.clear();
            activeRole = null;
            flipCount = 0;
            falseBreakoutCount = 0;
          }
        }
      }
    }

    // Emit the zone only if enough post-reset touches survived through the latest bar.
    if (activeMembers.size() >= SupportResistanceConfig.MIN_TOUCH_COUNT) {
      BigDecimal activeSum = BigDecimal.ZERO;
      for (SwingPoint activeMember : activeMembers) {
        activeSum = activeSum.add(activeMember.price());
      }
      BigDecimal finalCentroid =
          IndicatorMath.divide(activeSum, BigDecimal.valueOf(activeMembers.size()));
      BigDecimal finalBottom = IndicatorMath.publish(finalCentroid.subtract(halfZone));
      BigDecimal finalTop = IndicatorMath.publish(finalCentroid.add(halfZone));
      PriceBar latestBar = bars.getLast();
      LevelType finalRole = resolveLevelType(latestBar.close(), finalBottom, finalTop, activeRole);

      return new Bucket(
          latestBar.date(),
          finalBottom,
          finalTop,
          finalRole,
          activeMembers.size(),
          activeMembers.getFirst().date(),
          activeMembers.getLast().date());
    } else {
      return null;
    }
  }

  /**
   * Classifies a surviving zone's final polarity relative to the latest bar's closing price. Closes
   * strictly above the band classify as {@link LevelType#SUPPORT}, closes strictly below classify
   * as {@link LevelType#RESISTANCE}, and closes inside the band preserve the zone's active
   * structural role.
   */
  private LevelType resolveLevelType(
      BigDecimal latestClose, BigDecimal zoneBottom, BigDecimal zoneTop, LevelType activeRole) {
    if (latestClose.compareTo(zoneTop) > 0) {
      return LevelType.SUPPORT;
    } else if (latestClose.compareTo(zoneBottom) < 0) {
      return LevelType.RESISTANCE;
    } else {
      return activeRole;
    }
  }

  /** Loads latest source and calculated S&amp;R dates for all active tickers in one bulk query. */
  private List<Object[]> loadSyncStates(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailySupportResistanceRepository.findSyncStatesForActiveTickers();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklySupportResistanceRepository.findSyncStatesForActiveTickers();
    } else {
      log.error("Unsupported timeframe for loading S&R sync states: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  /** Loads chronologically ordered price bars for the ticker and timeframe. */
  private List<PriceBar> loadBars(Ticker ticker, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(PriceBar::from)
          .toList();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(PriceBar::from)
          .toList();
    } else {
      log.error("Unsupported timeframe for loading S&R bars: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  /** Deletes persisted S&amp;R snapshots older than the retention cutoff date. */
  private void deleteOlderSupportResistances(
      Ticker ticker, LocalDate cutoffDate, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      dailySupportResistanceRepository.deleteByTickerAndPriceDateLessThan(ticker, cutoffDate);
    } else if (timeframe == Timeframe.WEEKLY) {
      weeklySupportResistanceRepository.deleteByTickerAndPriceDateLessThan(ticker, cutoffDate);
    } else {
      log.error("Unsupported timeframe for S&R retention cleanup: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  /** Maps surviving {@link Bucket} zones to timeframe-specific JPA entities and persists them. */
  private void saveSupportResistances(Ticker ticker, List<Bucket> buckets, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      List<DailySupportResistance> toSave = new ArrayList<>();
      for (Bucket bucket : buckets) {
        toSave.add(
            DailySupportResistance.builder()
                .ticker(ticker)
                .priceDate(bucket.getPriceDate())
                .zoneBottom(bucket.getBottom())
                .zoneTop(bucket.getTop())
                .zoneMidpoint(bucket.getMidpoint())
                .touchCount(bucket.getTouchCount())
                .firstTouchDate(bucket.getFirstTouchDate())
                .lastTouchDate(bucket.getLastTouchDate())
                .levelType(bucket.getLevelType().name())
                .build());
      }
      dailySupportResistanceRepository.saveAll(toSave);

    } else if (timeframe == Timeframe.WEEKLY) {
      List<WeeklySupportResistance> toSave = new ArrayList<>();
      for (Bucket bucket : buckets) {
        toSave.add(
            WeeklySupportResistance.builder()
                .ticker(ticker)
                .priceDate(bucket.getPriceDate())
                .zoneBottom(bucket.getBottom())
                .zoneTop(bucket.getTop())
                .zoneMidpoint(bucket.getMidpoint())
                .touchCount(bucket.getTouchCount())
                .firstTouchDate(bucket.getFirstTouchDate())
                .lastTouchDate(bucket.getLastTouchDate())
                .levelType(bucket.getLevelType().name())
                .build());
      }
      weeklySupportResistanceRepository.saveAll(toSave);
    } else {
      log.error("Unsupported timeframe for saving S&R levels: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
