package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.Bucket;
import com.alphaflow.engine.calculators.enums.LevelType;
import com.alphaflow.engine.configs.SupportResistanceConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.engine.structure.DowAtrSwingDetector;
import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.SwingType;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@Component
@Slf4j
public class SupportResistanceCalculator {

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailySupportResistanceRepository dailySupportResistanceRepository;
  private final WeeklySupportResistanceRepository weeklySupportResistanceRepository;
  private final DowAtrSwingDetector dowAtrSwingDetector;

  @Autowired @Lazy private SupportResistanceCalculator self;

  public SupportResistanceCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository,
      DowAtrSwingDetector dowAtrSwingDetector) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
    this.dowAtrSwingDetector = dowAtrSwingDetector;
  }

  public void computeSupportResistances(Timeframe timeframe) {
    log.info("Computing {} support and resistances...", timeframe);
    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for {} S&R.", tickers.size(), timeframe);

    SupportResistanceCalculator proxy = (self != null) ? self : this;

    for (Ticker ticker : tickers) {
      try {
        proxy.computeSupportResistancesForTicker(ticker, timeframe);
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
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeSupportResistancesForTicker(Ticker ticker, Timeframe timeframe) {
    log.debug("Computing S&R for ticker {} and timeframe {}", ticker.getTickerSymbol(), timeframe);
    List<PriceBar> priceBars = loadBars(ticker, timeframe);
    if (!priceBars.isEmpty()) {
      LocalDate priceDate = priceBars.getLast().date();
      if (!isAlreadyComputed(ticker, priceDate, timeframe)) {
        LocalDate cutoffDate =
            priceDate.minusDays(SupportResistanceConfig.getRetentionDaysForTimeframe(timeframe));
        log.debug(
            "Purging {} S&R records older than {} for ticker {}.",
            timeframe,
            cutoffDate,
            ticker.getTickerSymbol());
        deleteOlderSupportResistances(ticker, cutoffDate, timeframe);

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
            "S&R already computed for ticker {} and timeframe {} for {}",
            ticker.getTickerSymbol(),
            timeframe,
            priceDate);
      }
    } else {
      log.debug(
          "No {} price bars found for ticker {}; skipping S&R computation.",
          timeframe,
          ticker.getTickerSymbol());
    }
  }

  public List<Bucket> computeBuckets(List<PriceBar> bars) {
    List<BigDecimal> atrSeries =
        IndicatorMath.calculateWilderAtrSeries(bars, SupportResistanceConfig.ATR_PERIOD);
    List<SwingPoint> swings =
        dowAtrSwingDetector.detectSwings(
            bars,
            atrSeries,
            SupportResistanceConfig.ATR_PERIOD,
            SupportResistanceConfig.SWING_ATR_MULTIPLIER);

    if (swings.size() >= SupportResistanceConfig.MIN_TOUCH_COUNT) {
      BigDecimal latestAtr = atrSeries.getLast();
      BigDecimal zoneWidth = latestAtr.multiply(SupportResistanceConfig.ZONE_ATR_MULTIPLIER);

      List<Bucket> candidates = buildCandidateZones(bars, atrSeries, swings, zoneWidth);
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
    selected.sort(Comparator.comparing(Bucket::getMidpoint).reversed());
    return selected;
  }

  private List<Bucket> buildCandidateZones(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      List<SwingPoint> swings,
      BigDecimal zoneWidth) {
    BigDecimal halfZone = IndicatorMath.divide(zoneWidth, BigDecimal.valueOf(2));

    List<Bucket> candidates = new ArrayList<>();
    for (SwingPoint anchor : swings) {
      BigDecimal windowBottom = anchor.price().subtract(halfZone);
      BigDecimal windowTop = anchor.price().add(halfZone);

      List<SwingPoint> members = new ArrayList<>();
      BigDecimal sumPrice = BigDecimal.ZERO;
      for (SwingPoint swing : swings) {
        if (swing.price().compareTo(windowBottom) >= 0
            && swing.price().compareTo(windowTop) <= 0) {
          members.add(swing);
          sumPrice = sumPrice.add(swing.price());
        }
      }

      if (members.size() >= SupportResistanceConfig.MIN_TOUCH_COUNT) {
        BigDecimal initialCentroid =
            IndicatorMath.divide(sumPrice, BigDecimal.valueOf(members.size()));
        BigDecimal initialBottom = IndicatorMath.publish(initialCentroid.subtract(halfZone));
        BigDecimal initialTop = IndicatorMath.publish(initialCentroid.add(halfZone));

        Bucket survivingZone =
            evaluateZoneLifecycle(bars, atrSeries, members, initialBottom, initialTop, halfZone);
        if (survivingZone != null) {
          candidates.add(survivingZone);
        }
      }
    }
    return candidates;
  }

  /**
   * Evaluates a candidate zone chronologically from its first pivot to the latest bar using
   * volatility-confirmed breaches ({@code BREACH_ATR_MULTIPLIER * ATR[i]}). Forgives minor false
   * breakouts up to {@code MAX_FALSE_BREAKOUTS}, allows up to {@code MAX_ALLOWED_FLIPS} clean
   * polarity flips, and resets active touch history whenever a zone is decisively broken or
   * whipsawed.
   */
  private Bucket evaluateZoneLifecycle(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      List<SwingPoint> members,
      BigDecimal zoneBottom,
      BigDecimal zoneTop,
      BigDecimal halfZone) {
    Map<Integer, SwingPoint> membersByBarIndex = new HashMap<>(members.size());
    for (SwingPoint member : members) {
      membersByBarIndex.put(member.barIndex(), member);
    }

    List<SwingPoint> activeMembers = new ArrayList<>();
    LevelType activeRole = null;
    int flipCount = 0;
    int falseBreakoutCount = 0;
    boolean inMildBreach = false;

    for (int i = members.getFirst().barIndex(); i < bars.size(); i++) {
      SwingPoint swingAtBar = membersByBarIndex.get(i);
      if (swingAtBar != null) {
        if (activeMembers.isEmpty()) {
          activeRole =
              (swingAtBar.type() == SwingType.LOW) ? LevelType.SUPPORT : LevelType.RESISTANCE;
        }
        activeMembers.add(swingAtBar);
      }

      if (!activeMembers.isEmpty()) {
        BigDecimal barClose = bars.get(i).close();
        BigDecimal breachMargin = atrSeries.get(i).multiply(SupportResistanceConfig.BREACH_ATR_MULTIPLIER);
        boolean isDecisiveBreach;
        boolean isMildBreach;

        if (activeRole == LevelType.SUPPORT) {
          isDecisiveBreach = barClose.compareTo(zoneBottom.subtract(breachMargin)) < 0;
          isMildBreach = barClose.compareTo(zoneBottom) < 0;
        } else {
          isDecisiveBreach = barClose.compareTo(zoneTop.add(breachMargin)) > 0;
          isMildBreach = barClose.compareTo(zoneTop) > 0;
        }

        if (isDecisiveBreach) {
          if (flipCount < SupportResistanceConfig.MAX_ALLOWED_FLIPS) {
            flipCount++;
            activeRole =
                (activeRole == LevelType.SUPPORT) ? LevelType.RESISTANCE : LevelType.SUPPORT;
            inMildBreach = false;
            falseBreakoutCount = 0;
          } else {
            activeMembers.clear();
            activeRole = null;
            flipCount = 0;
            falseBreakoutCount = 0;
            inMildBreach = false;
          }
        } else if (isMildBreach) {
          inMildBreach = true;
        } else if (inMildBreach) {
          falseBreakoutCount++;
          inMildBreach = false;
          if (falseBreakoutCount > SupportResistanceConfig.MAX_FALSE_BREAKOUTS) {
            activeMembers.clear();
            activeRole = null;
            flipCount = 0;
            falseBreakoutCount = 0;
          }
        }
      }
    }

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
      LevelType finalRole =
          resolveLevelType(latestBar.close(), finalBottom, finalTop, activeRole);

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

  private LevelType resolveLevelType(
      BigDecimal latestClose,
      BigDecimal zoneBottom,
      BigDecimal zoneTop,
      LevelType activeRole) {
    if (latestClose.compareTo(zoneTop) > 0) {
      return LevelType.SUPPORT;
    } else if (latestClose.compareTo(zoneBottom) < 0) {
      return LevelType.RESISTANCE;
    } else {
      return activeRole;
    }
  }

  private boolean isAlreadyComputed(Ticker ticker, LocalDate priceDate, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return !dailySupportResistanceRepository
          .findByTickerAndPriceDate(ticker, priceDate)
          .isEmpty();
    } else if (timeframe == Timeframe.WEEKLY) {
      return !weeklySupportResistanceRepository
          .findByTickerAndPriceDate(ticker, priceDate)
          .isEmpty();
    } else {
      log.error("Unsupported timeframe for S&R existence check: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

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
