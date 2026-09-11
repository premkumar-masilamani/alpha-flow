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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class WeeklyPriceCalculator {

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;

  @Autowired @Lazy private WeeklyPriceCalculator weeklyPriceCalculator;

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

    WeeklyPriceCalculator proxy = (weeklyPriceCalculator != null) ? weeklyPriceCalculator : this;

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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processTicker(Ticker ticker) {
    // Step 1: Load the daily bars with the computed start date (separate functions)
    Optional<LocalDate> startDateOpt = computeStartDate(ticker);
    if (startDateOpt.isEmpty()) {
      return;
    }
    LocalDate startDate = startDateOpt.get();
    List<DailyPrice> dailyBars = loadDailyBars(ticker, startDate);
    if (dailyBars.isEmpty()) {
      return;
    }

    // Step 2: Compute the weekly bars
    List<WeeklyPrice> weeklyBars = computeWeeklyBars(ticker, dailyBars);

    // Step 3: Save the weekly bars
    saveWeeklyBars(ticker, weeklyBars, startDate);
  }

  private Optional<LocalDate> computeStartDate(Ticker ticker) {
    Optional<WeeklyPrice> latestWeeklyOpt =
        weeklyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker);

    if (latestWeeklyOpt.isPresent()) {
      WeeklyPrice latestWeekly = latestWeeklyOpt.get();
      LocalDate calculationStartDate = latestWeekly.getPriceDate();
      log.debug(
          "Ticker {}: Starting weekly price computation from latest Monday to overwrite/update: {}",
          ticker.getTickerSymbol(),
          calculationStartDate);
      return Optional.of(calculationStartDate);
    }

    Optional<DailyPrice> earliestDailyOpt =
        dailyPriceRepository.findTopByTickerOrderByPriceDateAsc(ticker);
    if (earliestDailyOpt.isEmpty()) {
      log.warn(
          "Ticker {}: No daily prices found! Skipping weekly price computation.",
          ticker.getTickerSymbol());
      return Optional.empty();
    }

    LocalDate calculationStartDate =
        earliestDailyOpt
            .get()
            .getPriceDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    log.debug(
        "Ticker {}: No weekly prices found. Starting computation from earliest daily price date (aligned to Monday): {}",
        ticker.getTickerSymbol(),
        calculationStartDate);
    return Optional.of(calculationStartDate);
  }

  private List<DailyPrice> loadDailyBars(Ticker ticker, LocalDate startDate) {
    List<DailyPrice> dailyPrices =
        dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            ticker, startDate);

    if (dailyPrices.isEmpty()) {
      log.debug(
          "Ticker {}: No new daily prices found since {}.", ticker.getTickerSymbol(), startDate);
    }
    return dailyPrices;
  }

  private List<WeeklyPrice> computeWeeklyBars(Ticker ticker, List<DailyPrice> dailyBars) {
    if (dailyBars.isEmpty()) {
      return Collections.emptyList();
    }

    // Group daily prices by week-start Monday
    Map<LocalDate, List<DailyPrice>> dailyPricesByWeek =
        dailyBars.stream()
            .collect(
                Collectors.groupingBy(
                    dailyBar ->
                        dailyBar
                            .getPriceDate()
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));

    List<WeeklyPrice> weeklyPrices = new ArrayList<>();
    for (Map.Entry<LocalDate, List<DailyPrice>> entry : dailyPricesByWeek.entrySet()) {
      LocalDate weekMonday = entry.getKey();
      List<DailyPrice> weeklyPricesInWeek = entry.getValue();
      // Sort weekly daily prices by date asc to ensure correct open/close
      weeklyPricesInWeek.sort(Comparator.comparing(DailyPrice::getPriceDate));

      DailyPrice firstDay = weeklyPricesInWeek.getFirst();
      DailyPrice lastDay = weeklyPricesInWeek.getLast();
      BigDecimal open = firstDay.getPriceOpen();
      BigDecimal close = lastDay.getPriceClose();
      BigDecimal high =
          weeklyPricesInWeek.stream()
              .map(DailyPrice::getPriceHigh)
              .max(BigDecimal::compareTo)
              .orElse(firstDay.getPriceHigh());
      BigDecimal low =
          weeklyPricesInWeek.stream()
              .map(DailyPrice::getPriceLow)
              .min(BigDecimal::compareTo)
              .orElse(firstDay.getPriceLow());

      BigDecimal volume =
          weeklyPricesInWeek.stream()
              .map(DailyPrice::getVolume)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      WeeklyPrice weeklyPrice =
          WeeklyPrice.builder()
              .ticker(ticker)
              .priceDate(weekMonday)
              .priceOpen(open)
              .priceHigh(high)
              .priceLow(low)
              .priceClose(close)
              .volume(volume)
              .build();

      weeklyPrices.add(weeklyPrice);
    }

    weeklyPrices.sort(Comparator.comparing(WeeklyPrice::getPriceDate));
    return weeklyPrices;
  }

  private void saveWeeklyBars(Ticker ticker, List<WeeklyPrice> weeklyBars, LocalDate startDate) {
    if (weeklyBars.isEmpty()) {
      return;
    }

    // Fetch all existing weekly rows in the affected range once (avoids an N+1 query per week)
    Map<LocalDate, WeeklyPrice> existingWeeklyByMonday =
        weeklyPriceRepository.findByTickerAndPriceDateGreaterThanEqual(ticker, startDate).stream()
            .collect(Collectors.toMap(WeeklyPrice::getPriceDate, wp -> wp));

    List<WeeklyPrice> weeklyPricesToSave = new ArrayList<>();
    for (WeeklyPrice computed : weeklyBars) {
      WeeklyPrice existing = existingWeeklyByMonday.get(computed.getPriceDate());
      if (existing != null) {
        existing.setPriceOpen(computed.getPriceOpen());
        existing.setPriceHigh(computed.getPriceHigh());
        existing.setPriceLow(computed.getPriceLow());
        existing.setPriceClose(computed.getPriceClose());
        existing.setVolume(computed.getVolume());
        weeklyPricesToSave.add(existing);
      } else {
        weeklyPricesToSave.add(computed);
      }
    }

    weeklyPriceRepository.saveAll(weeklyPricesToSave);

    log.info(
        "{}: Saved/updated {} weekly prices.", ticker.getTickerSymbol(), weeklyPricesToSave.size());
  }
}
