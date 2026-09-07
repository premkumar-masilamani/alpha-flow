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
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${alphaflow.indicators.support-resistance.bucket-width-pct:0.01}")
  private double bucketWidthPct = 0.01;

  @Value("${alphaflow.indicators.support-resistance.max-buckets:20}")
  private int maxBuckets = 20;

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
    BigDecimal p =
        latest
            .high()
            .add(latest.low())
            .add(latest.close())
            .divide(BigDecimal.valueOf(3), 18, RoundingMode.HALF_UP);

    // 2. Calculate Linear Bucket Width (W)
    BigDecimal w = p.multiply(BigDecimal.valueOf(bucketWidthPct));

    // 3. Construct Fixed Linear Buckets
    List<Bucket> buckets = new ArrayList<>();

    // Resistance Buckets (k = 0 to maxBuckets - 1, above P)
    for (int k = 0; k < maxBuckets; k++) {
      BigDecimal bottom = p.add(w.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = p.add(w.multiply(BigDecimal.valueOf(k + 1)));
      buckets.add(new Bucket(priceDate, bottom, top, LevelType.RESISTANCE));
    }

    // Support Buckets (k = -1 to -maxBuckets, below P)
    for (int k = -1; k >= -maxBuckets; k--) {
      BigDecimal bottom = p.add(w.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = p.add(w.multiply(BigDecimal.valueOf(k + 1)));
      buckets.add(new Bucket(priceDate, bottom, top, LevelType.SUPPORT));
    }

    // Scan sequentially through historical candles
    for (PriceBar bar : bars) {
      for (Bucket b : buckets) {
        // STEP 1: Touch
        if (b.getLevelType() == LevelType.SUPPORT) {
          // If Daily Low >= Z_bottom AND Daily Low <= Z_top
          if (bar.low().compareTo(b.getBottom()) >= 0 && bar.low().compareTo(b.getTop()) <= 0) {
            b.incrementTouchCount();
          }
        } else {
          // RESISTANCE
          // If Daily High >= Z_bottom AND Daily High <= Z_top
          if (bar.high().compareTo(b.getBottom()) >= 0 && bar.high().compareTo(b.getTop()) <= 0) {
            b.incrementTouchCount();
          }
        }

        // STEP 2: Slice
        if (b.getLevelType() == LevelType.SUPPORT) {
          // If Daily Close < Z_bottom
          if (bar.close().compareTo(b.getBottom()) < 0) {
            b.resetTouchCount();
          }
        } else {
          // RESISTANCE
          // If Daily Close > Z_top
          if (bar.close().compareTo(b.getTop()) > 0) {
            b.resetTouchCount();
          }
        }
      }
    }

    // Filter proven buckets
    List<Bucket> proven = new ArrayList<>();
    for (Bucket b : buckets) {
      if (b.getTouchCount() >= 3) {
        // Role Reversal Check - strictly based on final Pivot P
        // If mid is below P -> SUPPORT, if above -> RESISTANCE
        if (b.getMidpoint().compareTo(p) < 0) {
          b.setLevelType(LevelType.SUPPORT);
        } else {
          b.setLevelType(LevelType.RESISTANCE);
        }
        proven.add(b);
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
      for (Bucket b : buckets) {
        toSave.add(
            DailySupportResistance.builder()
                .ticker(ticker)
                .priceDate(b.getPriceDate())
                .zoneBottom(b.getBottom())
                .zoneTop(b.getTop())
                .zoneMidpoint(b.getMidpoint())
                .touchCount(b.getTouchCount())
                .levelType(b.getLevelType().name())
                .build());
      }
      dailySupportResistanceRepository.saveAll(toSave);
    } else if (timeframe == Timeframe.WEEKLY) {
      List<WeeklySupportResistance> toSave = new ArrayList<>();
      for (Bucket b : buckets) {
        toSave.add(
            WeeklySupportResistance.builder()
                .ticker(ticker)
                .priceDate(b.getPriceDate())
                .zoneBottom(b.getBottom())
                .zoneTop(b.getTop())
                .zoneMidpoint(b.getMidpoint())
                .touchCount(b.getTouchCount())
                .levelType(b.getLevelType().name())
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

  private PriceBar toPriceBar(DailyPrice d) {
    return new PriceBar(
        d.getPriceDate(),
        d.getPriceOpen(),
        d.getPriceHigh(),
        d.getPriceLow(),
        d.getPriceClose(),
        d.getVolume());
  }

  private PriceBar toPriceBar(WeeklyPrice w) {
    return new PriceBar(
        w.getPriceDate(),
        w.getPriceOpen(),
        w.getPriceHigh(),
        w.getPriceLow(),
        w.getPriceClose(),
        w.getVolume());
  }
}
