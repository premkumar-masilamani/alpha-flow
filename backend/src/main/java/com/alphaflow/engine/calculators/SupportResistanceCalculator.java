package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.Bucket;
import com.alphaflow.engine.calculators.enums.LevelType;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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

  private static final double BUCKET_WIDTH_PCT = 0.01;
  private static final int MAX_BUCKETS = 20;
  private static final int BREAKOUT_CONFIRMATION_BARS = 2;
  private static final int MAX_FALSE_BREAKOUTS = 1;

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailySupportResistanceRepository dailySupportResistanceRepository;
  private final WeeklySupportResistanceRepository weeklySupportResistanceRepository;

  @Autowired @Lazy private SupportResistanceCalculator self;

  public SupportResistanceCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
  }

  public void computeSupportResistances() {
    log.info("Computing support and resistances...");
    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for S&R.", tickers.size());

    SupportResistanceCalculator proxy = (self != null) ? self : this;

    for (Ticker ticker : tickers) {
      try {
        proxy.computeSupportResistancesForTicker(ticker);
      } catch (Exception e) {
        log.error(
            "Failed to compute S&R for ticker {}: {}", ticker.getTickerSymbol(), e.getMessage(), e);
      }
    }
    log.info("Support and resistances computed.");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeSupportResistancesForTicker(Ticker ticker) {
    // Step 1: Load the bars (daily and weekly)
    List<PriceBar> dailyBars = loadBars(ticker, Timeframe.DAILY);
    List<PriceBar> weeklyBars = loadBars(ticker, Timeframe.WEEKLY);

    // Step 2: Compute the support and resistances (common)
    List<Bucket> dailyBuckets = computeBuckets(dailyBars);
    List<Bucket> weeklyBuckets = computeBuckets(weeklyBars);

    // Step 3: Save the computed support and resistances (daily and weekly)
    saveSupportResistances(ticker, Timeframe.DAILY, dailyBuckets);
    saveSupportResistances(ticker, Timeframe.WEEKLY, weeklyBuckets);
  }

  private List<PriceBar> loadBars(Ticker ticker, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(this::toPriceBar)
          .toList();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(this::toPriceBar)
          .toList();
    }
    log.error("Unsupported timeframe for loading bars: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
  }

  private List<Bucket> computeBuckets(List<PriceBar> bars) {
    if (bars.isEmpty()) {
      return Collections.emptyList();
    }

    PriceBar latest = bars.getLast();
    LocalDate priceDate = latest.date();

    // 1. Calculate Center Anchor (P)
    BigDecimal pivot =
        latest
            .high()
            .add(latest.low())
            .add(latest.close())
            .divide(BigDecimal.valueOf(3), 18, RoundingMode.HALF_UP);

    // 2. Calculate Linear Bucket Width (W)
    BigDecimal bucketWidth = pivot.multiply(BigDecimal.valueOf(BUCKET_WIDTH_PCT));

    // 3. Construct Fixed Linear Buckets
    List<Bucket> buckets = new ArrayList<>();

    // Resistance Buckets (k = 0 to MAX_BUCKETS - 1, above P)
    for (int k = 0; k < MAX_BUCKETS; k++) {
      BigDecimal bottom = pivot.add(bucketWidth.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = pivot.add(bucketWidth.multiply(BigDecimal.valueOf(k + 1)));
      buckets.add(new Bucket(priceDate, bottom, top, LevelType.RESISTANCE));
    }

    // Support Buckets (k = -1 to -MAX_BUCKETS, below P)
    for (int k = -1; k >= -MAX_BUCKETS; k--) {
      BigDecimal bottom = pivot.add(bucketWidth.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = pivot.add(bucketWidth.multiply(BigDecimal.valueOf(k + 1)));
      buckets.add(new Bucket(priceDate, bottom, top, LevelType.SUPPORT));
    }

    // Scan sequentially through historical candles
    int confirmationThreshold = Math.max(1, BREAKOUT_CONFIRMATION_BARS);
    int allowedFalseBreakouts = Math.max(0, MAX_FALSE_BREAKOUTS);

    for (PriceBar bar : bars) {
      for (Bucket bucket : buckets) {
        // STEP 1: Touch Detection
        // Only register touches if the level is not currently in a breach state
        if (bucket.getConsecutiveBreachCount() == 0) {
          if (bucket.getLevelType() == LevelType.SUPPORT) {
            if (bar.low().compareTo(bucket.getBottom()) >= 0
                && bar.low().compareTo(bucket.getTop()) <= 0
                && bar.close().compareTo(bucket.getBottom()) >= 0) {
              bucket.incrementTouchCount(bar.date());
            }
          } else if (bucket.getLevelType() == LevelType.RESISTANCE) {
            if (bar.high().compareTo(bucket.getBottom()) >= 0
                && bar.high().compareTo(bucket.getTop()) <= 0
                && bar.close().compareTo(bucket.getTop()) <= 0) {
              bucket.incrementTouchCount(bar.date());
            }
          }
        }

        // STEP 2: Slice with False Breakout Detection
        boolean isBreach = false;
        if (bucket.getLevelType() == LevelType.SUPPORT) {
          isBreach = bar.close().compareTo(bucket.getBottom()) < 0;
        } else if (bucket.getLevelType() == LevelType.RESISTANCE) {
          isBreach = bar.close().compareTo(bucket.getTop()) > 0;
        }

        if (isBreach) {
          if (bucket.getTouchCount() > 0) {
            bucket.incrementConsecutiveBreachCount();
            if (bucket.getConsecutiveBreachCount() >= confirmationThreshold
                || bucket.getFalseBreakoutCount() >= allowedFalseBreakouts) {
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

    // Filter proven buckets
    List<Bucket> proven = new ArrayList<>();
    for (Bucket bucket : buckets) {
      if (bucket.getTouchCount() >= 3) {
        // Role Reversal Check - strictly based on final Pivot P
        // If mid is below P -> SUPPORT, if above -> RESISTANCE
        if (bucket.getMidpoint().compareTo(pivot) < 0) {
          bucket.setLevelType(LevelType.SUPPORT);
        } else {
          bucket.setLevelType(LevelType.RESISTANCE);
        }
        proven.add(bucket);
      }
    }

    return proven;
  }

  private void saveSupportResistances(Ticker ticker, Timeframe timeframe, List<Bucket> buckets) {
    if (buckets.isEmpty()) {
      return;
    }

    LocalDate priceDate = buckets.getFirst().getPriceDate();
    if (isAlreadyComputed(ticker, priceDate, timeframe)) {
      return;
    }

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
      log.error("Unsupported timeframe for saving support resistance: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
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
    }
    log.error("Unsupported timeframe for checking existing support resistance: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
  }

  private PriceBar toPriceBar(DailyPrice dailyPrice) {
    return new PriceBar(
        dailyPrice.getPriceDate(),
        dailyPrice.getPriceOpen(),
        dailyPrice.getPriceHigh(),
        dailyPrice.getPriceLow(),
        dailyPrice.getPriceClose(),
        dailyPrice.getVolume());
  }

  private PriceBar toPriceBar(WeeklyPrice weeklyPrice) {
    return new PriceBar(
        weeklyPrice.getPriceDate(),
        weeklyPrice.getPriceOpen(),
        weeklyPrice.getPriceHigh(),
        weeklyPrice.getPriceLow(),
        weeklyPrice.getPriceClose(),
        weeklyPrice.getVolume());
  }
}
