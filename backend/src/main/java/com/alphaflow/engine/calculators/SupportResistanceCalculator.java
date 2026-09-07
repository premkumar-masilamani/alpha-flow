package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
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

  public enum LevelType {
    SUPPORT,
    RESISTANCE
  }

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

  private List<Bucket> computeSupportResistances(List<PriceBar> bars) {
    if (bars.isEmpty()) {
      return Collections.emptyList();
    }
    PriceBar latestBar = bars.getLast();
    return calculateProvenBuckets(bars, latestBar);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeSupportResistancesForTicker(Ticker ticker) {
    // Step 1: Load the bars (daily and weekly)
    List<PriceBar> dailyBars = loadBars(ticker, Timeframe.DAILY);
    List<PriceBar> weeklyBars = loadBars(ticker, Timeframe.WEEKLY);

    // Step 2: Compute the support and resistances (common)
    List<Bucket> dailyBuckets = computeSupportResistances(dailyBars);
    List<Bucket> weeklyBuckets = computeSupportResistances(weeklyBars);

    // Step 3: Save the computed support and resistances (daily and weekly)
    saveSupportResistances(ticker, Timeframe.DAILY, dailyBuckets, dailyBars);
    saveSupportResistances(ticker, Timeframe.WEEKLY, weeklyBuckets, weeklyBars);
  }

  private void saveSupportResistances(
      Ticker ticker, Timeframe timeframe, List<Bucket> buckets, List<PriceBar> bars) {
    if (bars.isEmpty()) {
      return;
    }

    LocalDate priceDate = bars.getLast().date();
    if (isAlreadyComputed(ticker, priceDate, timeframe)) {
      return;
    }

    if (timeframe == Timeframe.DAILY) {
      List<DailySupportResistance> toSave = new ArrayList<>();
      for (Bucket b : buckets) {
        toSave.add(
            DailySupportResistance.builder()
                .ticker(ticker)
                .priceDate(priceDate)
                .zoneBottom(b.bottom)
                .zoneTop(b.top)
                .zoneMidpoint(b.midpoint)
                .touchCount(b.touchCount)
                .levelType(b.levelType.name())
                .build());
      }
      dailySupportResistanceRepository.saveAll(toSave);
    } else if (timeframe == Timeframe.WEEKLY) {
      List<WeeklySupportResistance> toSave = new ArrayList<>();
      for (Bucket b : buckets) {
        toSave.add(
            WeeklySupportResistance.builder()
                .ticker(ticker)
                .priceDate(priceDate)
                .zoneBottom(b.bottom)
                .zoneTop(b.top)
                .zoneMidpoint(b.midpoint)
                .touchCount(b.touchCount)
                .levelType(b.levelType.name())
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

  private List<Bucket> calculateProvenBuckets(List<PriceBar> bars, PriceBar latestBar) {
    // 1. Calculate Center Anchor (P)
    BigDecimal p =
        latestBar
            .high()
            .add(latestBar.low())
            .add(latestBar.close())
            .divide(BigDecimal.valueOf(3), 18, RoundingMode.HALF_UP);

    // 2. Calculate Linear Bucket Width (W)
    BigDecimal w = p.multiply(BigDecimal.valueOf(bucketWidthPct));

    // 3. Construct Fixed Linear Buckets
    List<Bucket> buckets = new ArrayList<>();

    // Resistance Buckets (k = 0 to maxBuckets - 1, above P)
    for (int k = 0; k < maxBuckets; k++) {
      BigDecimal bottom = p.add(w.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = p.add(w.multiply(BigDecimal.valueOf(k + 1)));
      buckets.add(new Bucket(bottom, top, LevelType.RESISTANCE));
    }

    // Support Buckets (k = -1 to -maxBuckets, below P)
    for (int k = -1; k >= -maxBuckets; k--) {
      BigDecimal bottom = p.add(w.multiply(BigDecimal.valueOf(k)));
      BigDecimal top = p.add(w.multiply(BigDecimal.valueOf(k + 1)));
      buckets.add(new Bucket(bottom, top, LevelType.SUPPORT));
    }

    // Scan sequentially through historical candles
    for (PriceBar bar : bars) {
      for (Bucket b : buckets) {
        // STEP 1: Touch
        if (b.levelType == LevelType.SUPPORT) {
          // If Daily Low >= Z_bottom AND Daily Low <= Z_top
          if (bar.low().compareTo(b.bottom) >= 0 && bar.low().compareTo(b.top) <= 0) {
            b.touchCount++;
          }
        } else {
          // RESISTANCE
          // If Daily High >= Z_bottom AND Daily High <= Z_top
          if (bar.high().compareTo(b.bottom) >= 0 && bar.high().compareTo(b.top) <= 0) {
            b.touchCount++;
          }
        }

        // STEP 2: Slice
        if (b.levelType == LevelType.SUPPORT) {
          // If Daily Close < Z_bottom
          if (bar.close().compareTo(b.bottom) < 0) {
            b.touchCount = 0;
          }
        } else {
          // RESISTANCE
          // If Daily Close > Z_top
          if (bar.close().compareTo(b.top) > 0) {
            b.touchCount = 0;
          }
        }
      }
    }

    // Filter proven buckets
    List<Bucket> proven = new ArrayList<>();
    for (Bucket b : buckets) {
      if (b.touchCount >= 3) {
        // Role Reversal Check - strictly based on final Pivot P
        // If mid is below P -> SUPPORT, if above -> RESISTANCE
        if (b.midpoint.compareTo(p) < 0) {
          b.levelType = LevelType.SUPPORT;
        } else {
          b.levelType = LevelType.RESISTANCE;
        }
        proven.add(b);
      }
    }

    return proven;
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

  private static class Bucket {
    BigDecimal bottom;
    BigDecimal top;
    BigDecimal midpoint;
    LevelType levelType;
    int touchCount;

    Bucket(BigDecimal bottom, BigDecimal top, LevelType levelType) {
      this.bottom = bottom;
      this.top = top;
      this.midpoint = bottom.add(top).divide(BigDecimal.valueOf(2), 18, RoundingMode.HALF_UP);
      this.levelType = levelType;
      this.touchCount = 0;
    }
  }
}
