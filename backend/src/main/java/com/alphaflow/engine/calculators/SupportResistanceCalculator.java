package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.Bucket;
import com.alphaflow.engine.calculators.enums.LevelType;
import com.alphaflow.engine.configs.SupportResistanceConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.entities.*;
import com.alphaflow.persistence.repositories.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
  private final SupportResistanceConfig supportResistanceConfig;

  @Autowired @Lazy private SupportResistanceCalculator self;

  public SupportResistanceCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository,
      SupportResistanceConfig supportResistanceConfig) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
    this.supportResistanceConfig = supportResistanceConfig;
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
            exception.getMessage());
      }
    }
    log.info("{} support and resistances computed.", timeframe);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeSupportResistancesForTicker(Ticker ticker, Timeframe timeframe) {
    log.debug("Computing S&R for ticker {} and timeframe {}", ticker.getTickerSymbol(), timeframe);
    // Step 1: Load the bars
    List<PriceBar> priceBars = loadBars(ticker, timeframe);
    if (!priceBars.isEmpty()) {
      LocalDate priceDate = priceBars.getLast().date();
      if (!isAlreadyComputed(ticker, priceDate, timeframe)) {
        // Step 2: Purge older records
        LocalDate cutoffDate =
            priceDate.minusDays(supportResistanceConfig.getRetentionDaysForTimeframe(timeframe));
        deleteOlderSupportResistances(ticker, cutoffDate, timeframe);

        // Step 3: Compute the support and resistances
        List<Bucket> buckets = computeBuckets(priceBars);

        // Step 4: Save the computed support and resistances
        if (!buckets.isEmpty()) {
          saveSupportResistances(ticker, buckets, timeframe);
        }
      } else {
        log.debug(
            "S&R already computed for ticker {} and timeframe {} for {}",
            ticker.getTickerSymbol(),
            timeframe,
            priceDate);
      }
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
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  public List<Bucket> computeBuckets(List<PriceBar> bars) {
    PriceBar latest = bars.getLast();
    LocalDate priceDate = latest.date();

    // 1. Calculate Center Anchor (Pivot)
    BigDecimal pivot =
        latest
            .high()
            .add(latest.low())
            .add(latest.close())
            .divide(BigDecimal.valueOf(3), 18, RoundingMode.HALF_UP);

    // 2. Calculate Adaptive Bucket Width
    BigDecimal bucketWidth;
    if (bars.size() >= supportResistanceConfig.getAtrPeriod()) {
      BigDecimal atr = calculateAtr(bars, supportResistanceConfig.getAtrPeriod());
      bucketWidth =
          atr.multiply(BigDecimal.valueOf(supportResistanceConfig.getAtrMultiplier()))
              .setScale(4, RoundingMode.HALF_UP);
    } else {
      bucketWidth =
          latest
              .close()
              .multiply(BigDecimal.valueOf(supportResistanceConfig.getBucketWidthPct()))
              .setScale(4, RoundingMode.HALF_UP);
    }

    BigDecimal step =
        bucketWidth.multiply(BigDecimal.valueOf(supportResistanceConfig.getStrideRatio()));

    // 3. Compute dynamic historical price range
    BigDecimal minPrice =
        bars.stream().map(PriceBar::low).min(BigDecimal::compareTo).orElse(latest.low());

    BigDecimal maxPrice =
        bars.stream().map(PriceBar::high).max(BigDecimal::compareTo).orElse(latest.high());

    BigDecimal maxDiff = maxPrice.subtract(pivot).max(BigDecimal.ZERO);
    int upwardBucketCount =
        Math.min(
            maxDiff.divide(step, 0, RoundingMode.CEILING).intValueExact(),
            supportResistanceConfig.getMaxAllowedBuckets());

    BigDecimal minDiff = pivot.subtract(minPrice).max(BigDecimal.ZERO);
    int downwardBucketCount =
        Math.min(
            minDiff.divide(step, 0, RoundingMode.CEILING).intValueExact(),
            supportResistanceConfig.getMaxAllowedBuckets());

    // 4. Construct Dynamic Overlapping Buckets
    List<Bucket> buckets = new ArrayList<>();

    // Resistance Buckets (k = 0 to upwardBucketCount - 1, above pivot)
    for (int k = 0; k < upwardBucketCount; k++) {
      BigDecimal bottom = pivot.add(step.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = bottom.add(bucketWidth);
      buckets.add(new Bucket(priceDate, bottom, top, LevelType.RESISTANCE));
    }

    // Support Buckets (k = 1 to downwardBucketCount, below pivot)
    for (int k = 1; k <= downwardBucketCount; k++) {
      BigDecimal bottom = pivot.subtract(step.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = bottom.add(bucketWidth);
      buckets.add(new Bucket(priceDate, bottom, top, LevelType.SUPPORT));
    }

    // Scan sequentially through historical candles
    for (PriceBar bar : bars) {
      for (Bucket bucket : buckets) {
        // STEP 1: Touch Detection
        // Only register touches if the level is not currently in a breach state
        if (bucket.getConsecutiveBreachCount() == 0) {
          if (bucket.getLevelType() == LevelType.SUPPORT) {
            if (bar.low().compareTo(bucket.getBottom()) >= 0
                && bar.low().compareTo(bucket.getTop()) <= 0) {
              bucket.incrementTouchCount(bar.date());
            }
          } else {
            if (bar.high().compareTo(bucket.getBottom()) >= 0
                && bar.high().compareTo(bucket.getTop()) <= 0) {
              bucket.incrementTouchCount(bar.date());
            }
          }
        }

        // STEP 2: Slice with False Breakout Detection
        boolean isBreach;
        if (bucket.getLevelType() == LevelType.SUPPORT) {
          isBreach = bar.close().compareTo(bucket.getBottom()) < 0;
        } else {
          isBreach = bar.close().compareTo(bucket.getTop()) > 0;
        }

        if (isBreach) {
          if (bucket.getTouchCount() > 0) {
            bucket.incrementConsecutiveBreachCount();
            if (bucket.getConsecutiveBreachCount()
                    >= supportResistanceConfig.getBreakoutConfirmationBars()
                || bucket.getFalseBreakoutCount()
                    >= supportResistanceConfig.getMaxFalseBreakouts()) {
              bucket.resetTouchCount();
            }
          }
        } else {
          if (bucket.getConsecutiveBreachCount() > 0) {
            // Reclaim from false breakout: preserve level, record the false breakout event,
            // but do NOT credit a touch point for the false breakout
            bucket.incrementFalseBreakoutCount();
            bucket.resetConsecutiveBreachCount();
          }
        }
      }
    }

    // Filter proven candidate buckets
    List<Bucket> provenCandidates = new ArrayList<>();
    for (Bucket bucket : buckets) {
      if (bucket.getTouchCount() >= supportResistanceConfig.getMinTouchCount()) {
        provenCandidates.add(bucket);
      }
    }

    // Apply Non-Maximum Suppression (NMS)
    List<Bucket> deduplicatedBuckets = applyNonMaximumSuppression(provenCandidates);

    // Role Reversal Check - strictly based on latest candle close
    for (Bucket bucket : deduplicatedBuckets) {
      if (bucket.getMidpoint().compareTo(latest.close()) < 0) {
        bucket.setLevelType(LevelType.SUPPORT);
      } else {
        bucket.setLevelType(LevelType.RESISTANCE);
      }
    }

    return deduplicatedBuckets;
  }

  public BigDecimal calculateAtr(List<PriceBar> bars, int period) {
    return IndicatorMath.calculateAtr(bars, period);
  }

  public List<Bucket> applyNonMaximumSuppression(List<Bucket> candidates) {
    List<Bucket> sorted =
        candidates.stream()
            .sorted(
                Comparator.comparingInt(Bucket::getTouchCount)
                    .reversed()
                    .thenComparing(
                        Bucket::getLastTouchDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    List<Bucket> selected = new ArrayList<>();
    BigDecimal threshold = BigDecimal.valueOf(supportResistanceConfig.getNmsOverlapThreshold());

    for (Bucket candidate : sorted) {
      boolean overlaps = false;
      for (Bucket kept : selected) {
        BigDecimal overlapBottom = candidate.getBottom().max(kept.getBottom());
        BigDecimal overlapTop = candidate.getTop().min(kept.getTop());
        if (overlapTop.compareTo(overlapBottom) > 0) {
          BigDecimal overlapWidth = overlapTop.subtract(overlapBottom);
          BigDecimal candidateWidth = candidate.getTop().subtract(candidate.getBottom());
          BigDecimal overlapRatio = overlapWidth.divide(candidateWidth, 4, RoundingMode.HALF_UP);
          if (overlapRatio.compareTo(threshold) >= 0) {
            overlaps = true;
            break;
          }
        }
      }
      if (!overlaps) {
        selected.add(candidate);
      }
    }
    return selected;
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
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
