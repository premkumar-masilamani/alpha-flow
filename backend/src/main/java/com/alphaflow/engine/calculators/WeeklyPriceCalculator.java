package com.alphaflow.engine.calculators;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WeeklyPriceCalculator {

  private static final Logger log = LoggerFactory.getLogger(WeeklyPriceCalculator.class);
  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;

  @Autowired @Lazy private WeeklyPriceCalculator self;

  public WeeklyPriceCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
  }

  public void computeWeeklyPrices() {
    log.info("Starting weekly price computation...");

    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for weekly prices.", tickers.size());

    WeeklyPriceCalculator proxy = (self != null) ? self : this;
    for (Ticker ticker : tickers) {
      try {
        proxy.processTicker(ticker);
      } catch (Exception e) {
        log.error(
            "Failed to compute weekly prices for ticker {}: {}",
            ticker.getTickerSymbol(),
            e.getMessage(),
            e);
      }
    }

    log.info("Weekly price computation completed.");
  }

  @Transactional
  public void processTicker(Ticker ticker) {
    // 1. Determine start date
    Optional<WeeklyPrice> latestWeeklyOpt =
        weeklyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker);
    LocalDate calculationStartDate;

    if (latestWeeklyOpt.isPresent()) {
      WeeklyPrice latestWeekly = latestWeeklyOpt.get();
      calculationStartDate = latestWeekly.getPriceDate();
      log.debug(
          "Ticker {}: Starting weekly price computation from latest Monday to overwrite/update: {}",
          ticker.getTickerSymbol(),
          calculationStartDate);
    } else {
      Optional<DailyPrice> firstDailyOpt =
          dailyPriceRepository.findTopByTickerOrderByPriceDateAsc(ticker);
      if (firstDailyOpt.isEmpty()) {
        log.warn(
            "Ticker {}: No daily prices found! Skipping weekly price computation.",
            ticker.getTickerSymbol());
        return;
      }
      calculationStartDate =
          firstDailyOpt
              .get()
              .getPriceDate()
              .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      log.debug(
          "Ticker {}: No weekly prices found. Starting computation from first daily price date (aligned to Monday): {}",
          ticker.getTickerSymbol(),
          calculationStartDate);
    }

    // 2. Fetch daily prices starting from calculationStartDate
    List<DailyPrice> dailyPrices =
        dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            ticker, calculationStartDate);

    if (dailyPrices.isEmpty()) {
      log.debug(
          "Ticker {}: No new daily prices found since {}.",
          ticker.getTickerSymbol(),
          calculationStartDate);
      return;
    }

    // 3. Group daily prices by week-start Monday
    Map<LocalDate, List<DailyPrice>> dailyPricesByWeek =
        dailyPrices.stream()
            .collect(
                Collectors.groupingBy(
                    c ->
                        c.getPriceDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));

    // Fetch all existing weekly rows in the affected range once (avoids an N+1 query per week)
    Map<LocalDate, WeeklyPrice> existingWeeklyByMonday =
        weeklyPriceRepository
            .findByTickerAndPriceDateGreaterThanEqual(ticker, calculationStartDate)
            .stream()
            .collect(Collectors.toMap(WeeklyPrice::getPriceDate, wp -> wp));

    List<WeeklyPrice> weeklyPricesToSave = new ArrayList<>();
    for (Map.Entry<LocalDate, List<DailyPrice>> entry : dailyPricesByWeek.entrySet()) {
      LocalDate weekMonday = entry.getKey();
      List<DailyPrice> weeklyPrices = entry.getValue();
      // Sort weekly daily prices by date asc to ensure correct open/close
      weeklyPrices.sort(Comparator.comparing(DailyPrice::getPriceDate));

      DailyPrice firstDay = weeklyPrices.getFirst();
      DailyPrice lastDay = weeklyPrices.getLast();
      BigDecimal open = firstDay.getPriceOpen();
      BigDecimal close = lastDay.getPriceClose();
      BigDecimal high = firstDay.getPriceHigh();
      BigDecimal low = firstDay.getPriceLow();

      for (DailyPrice dp : weeklyPrices) {
        if (dp.getPriceHigh().compareTo(high) > 0) {
          high = dp.getPriceHigh();
        }
        if (dp.getPriceLow().compareTo(low) < 0) {
          low = dp.getPriceLow();
        }
      }

      BigDecimal volume =
          weeklyPrices.stream().map(DailyPrice::getVolume).reduce(BigDecimal.ZERO, BigDecimal::add);
      // Look up existing weekly price record (from the pre-fetched map) to update or insert
      WeeklyPrice weeklyPrice =
          existingWeeklyByMonday.getOrDefault(
              weekMonday, WeeklyPrice.builder().ticker(ticker).priceDate(weekMonday).build());

      weeklyPrice.setPriceOpen(open);
      weeklyPrice.setPriceHigh(high);
      weeklyPrice.setPriceLow(low);
      weeklyPrice.setPriceClose(close);
      weeklyPrice.setVolume(volume);
      weeklyPricesToSave.add(weeklyPrice);
    }

    weeklyPriceRepository.saveAll(weeklyPricesToSave);

    log.info(
        "Ticker {}: Saved/updated {} weekly prices.",
        ticker.getTickerSymbol(),
        weeklyPricesToSave.size());
  }
}
