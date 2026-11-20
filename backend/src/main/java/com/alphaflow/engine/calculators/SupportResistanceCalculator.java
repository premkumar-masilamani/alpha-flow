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
  private final DowAtrSwingDetector dowAtrSwingDetector;

  @Autowired @Lazy private SupportResistanceCalculator self;

  public SupportResistanceCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository,
      SupportResistanceConfig supportResistanceConfig,
      DowAtrSwingDetector dowAtrSwingDetector) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
    this.supportResistanceConfig = supportResistanceConfig;
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
            priceDate.minusDays(supportResistanceConfig.getRetentionDaysForTimeframe(timeframe));
        deleteOlderSupportResistances(ticker, cutoffDate, timeframe);

        List<Bucket> buckets = computeBuckets(priceBars);

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

  public List<Bucket> computeBuckets(List<PriceBar> bars) {
    int atrPeriod = supportResistanceConfig.getAtrPeriod();
    List<SwingPoint> swings =
        dowAtrSwingDetector.detectSwings(
            bars, atrPeriod, supportResistanceConfig.getSwingAtrMultiplier());

    if (swings.size() >= supportResistanceConfig.getMinTouchCount()) {
      List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(bars, atrPeriod);
      BigDecimal latestAtr = atrSeries.getLast();
      BigDecimal zoneWidth = latestAtr.multiply(supportResistanceConfig.getZoneAtrMultiplier());

      if (zoneWidth.signum() > 0) {
        PriceBar latestBar = bars.getLast();
        List<Bucket> candidates = buildCandidateZones(swings, latestBar, zoneWidth);
        return applyNonOverlappingSuppression(candidates);
      } else {
        log.debug("Computed zone width is non-positive ({}); skipping S&R generation.", zoneWidth);
        return List.of();
      }
    } else {
      log.debug(
          "Insufficient confirmed Dow swings ({} < {}) to form S&R zones.",
          swings.size(),
          supportResistanceConfig.getMinTouchCount());
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
      List<SwingPoint> swings, PriceBar latestBar, BigDecimal zoneWidth) {
    BigDecimal halfZone = IndicatorMath.divide(zoneWidth, BigDecimal.valueOf(2));
    LocalDate priceDate = latestBar.date();
    BigDecimal latestClose = latestBar.close();
    int minTouchCount = supportResistanceConfig.getMinTouchCount();

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

      if (members.size() >= minTouchCount) {
        BigDecimal centroid = IndicatorMath.divide(sumPrice, BigDecimal.valueOf(members.size()));
        BigDecimal zoneBottom = IndicatorMath.publish(centroid.subtract(halfZone));
        BigDecimal zoneTop = IndicatorMath.publish(centroid.add(halfZone));
        SwingPoint firstMember = members.getFirst();
        SwingPoint lastMember = members.getLast();
        LevelType levelType =
            resolveLevelType(latestClose, zoneBottom, zoneTop, lastMember.type());

        candidates.add(
            new Bucket(
                priceDate,
                zoneBottom,
                zoneTop,
                levelType,
                members.size(),
                firstMember.date(),
                lastMember.date()));
      }
    }
    return candidates;
  }

  private LevelType resolveLevelType(
      BigDecimal latestClose,
      BigDecimal zoneBottom,
      BigDecimal zoneTop,
      SwingType mostRecentSwingType) {
    if (latestClose.compareTo(zoneTop) > 0) {
      return LevelType.SUPPORT;
    } else if (latestClose.compareTo(zoneBottom) < 0) {
      return LevelType.RESISTANCE;
    } else if (mostRecentSwingType == SwingType.LOW) {
      return LevelType.SUPPORT;
    } else {
      return LevelType.RESISTANCE;
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
