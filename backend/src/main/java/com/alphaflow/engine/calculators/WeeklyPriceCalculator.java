package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.PipelineConfig;
import com.alphaflow.engine.pipeline.PipelineJob;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class WeeklyPriceCalculator implements PipelineJob {

  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final TickerRepository tickerRepository;

  @Autowired @Lazy private WeeklyPriceCalculator weeklyPriceCalculator;

  public WeeklyPriceCalculator(
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      TickerRepository tickerRepository) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.tickerRepository = tickerRepository;
  }

  @Override
  public String getJobName() {
    return PipelineConfig.STEP_CALCULATE_WEEKLY_PRICE;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    return weeklyPriceRepository.hasPendingWeeklyPrices();
  }

  @Override
  public void execute(Timeframe timeframe) {
    log.info("Starting weekly price computation...");

    List<Ticker> activeTickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for weekly prices.", activeTickers.size());

    Map<Ticker, LocalDate> latestDates =
        weeklyPriceRepository.findLatestPriceDatesForActiveTickers();

    WeeklyPriceCalculator proxy = (weeklyPriceCalculator != null) ? weeklyPriceCalculator : this;

    for (Ticker ticker : activeTickers) {
      try {
        LocalDate latestCalculatedDate = latestDates.get(ticker);
        proxy.processTicker(ticker, latestCalculatedDate);
      } catch (Exception exception) {
        log.error(
            "Failed to compute weekly prices for ticker {}: {}",
            ticker.getTickerSymbol(),
            exception.getMessage(),
            exception);
      }
    }

    log.info("Weekly price computation completed.");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processTicker(Ticker ticker, LocalDate latestCalculatedDate) {
    List<DailyPrice> dailyBars = loadDailyBars(ticker, latestCalculatedDate);
    if (!dailyBars.isEmpty()) {
      LocalDate startDate =
          (latestCalculatedDate != null)
              ? latestCalculatedDate
              : dailyBars
                  .getFirst()
                  .getPriceDate()
                  .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      List<WeeklyPrice> weeklyBars = computeWeeklyBars(ticker, dailyBars);
      saveWeeklyBars(ticker, weeklyBars, startDate);
    } else {
      log.debug(
          "Ticker {}: No daily prices found to aggregate into weekly bars (since={}).",
          ticker.getTickerSymbol(),
          latestCalculatedDate);
    }
  }

  private List<DailyPrice> loadDailyBars(Ticker ticker, LocalDate latestCalculatedDate) {
    if (latestCalculatedDate != null) {
      return dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
          ticker, latestCalculatedDate);
    } else {
      return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker);
    }
  }

  private List<WeeklyPrice> computeWeeklyBars(Ticker ticker, List<DailyPrice> dailyBars) {
    // Group daily prices by week-start Monday
    Map<LocalDate, List<DailyPrice>> dailyPricesByWeek =
        dailyBars.stream()
            .collect(
                Collectors.groupingBy(dailyBar ->
                    dailyBar
                        .getPriceDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));

    List<WeeklyPrice> weeklyPrices = new ArrayList<>();
    for (Map.Entry<LocalDate, List<DailyPrice>> entry : dailyPricesByWeek.entrySet()) {
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

      LocalDate weekMonday = entry.getKey();
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
    // Fetch all existing weekly rows in the affected range once (avoids an N+1 query per week)
    Map<LocalDate, WeeklyPrice> existingWeeklyByMonday =
        weeklyPriceRepository.findByTickerAndPriceDateGreaterThanEqual(ticker, startDate).stream()
            .collect(Collectors.toMap(WeeklyPrice::getPriceDate, Function.identity()));

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
