package com.alphaflow.engine.calculators;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes and persists weekly prices for a single ticker.
 * <p>
 * This lives in its own bean (rather than as a method on {@link WeeklyPriceCalculator})
 * so that the {@link Transactional} boundary is honored: a self-invocation from within
 * the same bean would bypass Spring's proxy and silently disable the transaction.
 */
@Component
public class WeeklyTickerProcessor {

    private static final Logger log = LoggerFactory.getLogger(WeeklyTickerProcessor.class);

    private final DailyPriceRepository dailyPriceRepository;
    private final WeeklyPriceRepository weeklyPriceRepository;

    public WeeklyTickerProcessor(
            DailyPriceRepository dailyPriceRepository,
            WeeklyPriceRepository weeklyPriceRepository
    ) {
        this.dailyPriceRepository = dailyPriceRepository;
        this.weeklyPriceRepository = weeklyPriceRepository;
    }

    @Transactional
    public void processTicker(Ticker ticker) {
        // 1. Determine start date
        Optional<WeeklyPrice> latestWeeklyOpt = weeklyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker);
        LocalDate calculationStartDate;

        if (latestWeeklyOpt.isPresent()) {
            WeeklyPrice latestWeekly = latestWeeklyOpt.get();
            calculationStartDate = latestWeekly.getPriceDate();
            log.debug("Ticker {}: Starting weekly price computation from latest Monday to overwrite/update: {}",
                    ticker.getTickerSymbol(), calculationStartDate);
        } else {
            // Default to the first daily price date (aligned to Monday of that week) or a fallback date if no daily prices exist
            calculationStartDate = dailyPriceRepository.findTopByTickerOrderByPriceDateAsc(ticker)
                    .map(DailyPrice::getPriceDate)
                    .orElse(LocalDate.of(1900, 1, 1))
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            log.debug("Ticker {}: No weekly prices found. Starting computation from first daily price date (aligned to Monday): {}",
                    ticker.getTickerSymbol(), calculationStartDate);
        }

        // 2. Fetch daily prices starting from calculationStartDate
        List<DailyPrice> dailyPrices = dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(ticker, calculationStartDate);
        if (dailyPrices.isEmpty()) {
            log.debug("Ticker {}: No new daily prices found since {}.", ticker.getTickerSymbol(), calculationStartDate);
            return;
        }

        // 3. Group daily prices by week-start Monday
        Map<LocalDate, List<DailyPrice>> dailyPricesByWeek = dailyPrices.stream()
                .collect(Collectors.groupingBy(c -> c.getPriceDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));

        // Fetch all existing weekly rows in the affected range once (avoids an N+1 query per week)
        Map<LocalDate, WeeklyPrice> existingWeeklyByMonday = weeklyPriceRepository
                .findByTickerAndPriceDateGreaterThanEqual(ticker, calculationStartDate).stream()
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
            BigDecimal high = weeklyPrices.stream().map(DailyPrice::getPriceHigh).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal low = weeklyPrices.stream().map(DailyPrice::getPriceLow).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            long volume = weeklyPrices.stream().mapToLong(DailyPrice::getVolume).sum();

            // Look up existing weekly price record (from the pre-fetched map) to update or insert
            WeeklyPrice weeklyPrice = existingWeeklyByMonday.getOrDefault(weekMonday,
                    WeeklyPrice.builder()
                            .ticker(ticker)
                            .priceDate(weekMonday)
                            .build());

            weeklyPrice.setPriceOpen(open);
            weeklyPrice.setPriceHigh(high);
            weeklyPrice.setPriceLow(low);
            weeklyPrice.setPriceClose(close);
            weeklyPrice.setVolume(volume);

            weeklyPricesToSave.add(weeklyPrice);
        }

        if (!weeklyPricesToSave.isEmpty()) {
            weeklyPriceRepository.saveAll(weeklyPricesToSave);
            log.info("Ticker {}: Saved/updated {} weekly prices.", ticker.getTickerSymbol(), weeklyPricesToSave.size());
        }
    }
}
