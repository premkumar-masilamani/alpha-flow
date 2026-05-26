package com.alphaflow.api.services;

import com.alphaflow.infrastructure.entities.DailyCandleData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.entities.WeeklyCandleData;
import com.alphaflow.infrastructure.repositories.DailyCandleDataRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import com.alphaflow.infrastructure.repositories.WeeklyCandleDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyCandleService {

    @Autowired
    @Lazy
    private WeeklyCandleService self;

    private static final Logger log = LoggerFactory.getLogger(WeeklyCandleService.class);

    private final TickerRepository tickerRepository;
    private final DailyCandleDataRepository dailyCandleDataRepository;
    private final WeeklyCandleDataRepository weeklyCandleDataRepository;

    public WeeklyCandleService(
            TickerRepository tickerRepository,
            DailyCandleDataRepository dailyCandleDataRepository,
            WeeklyCandleDataRepository weeklyCandleDataRepository
    ) {
        this.tickerRepository = tickerRepository;
        this.dailyCandleDataRepository = dailyCandleDataRepository;
        this.weeklyCandleDataRepository = weeklyCandleDataRepository;
    }

    public void computeWeeklyCandles() {
        log.info("Starting weekly candlestick computation process...");

        List<Ticker> tickers = tickerRepository.findAll();
        log.info("Found {} tickers to process.", tickers.size());

        for (Ticker ticker : tickers) {
            try {
                self.processTicker(ticker);
            } catch (Exception e) {
                log.error("Failed to compute weekly candles for ticker {}: {}", ticker.getTickerSymbol(), e.getMessage(), e);
            }
        }

        log.info("Weekly candlestick computation process completed.");
    }

    @Transactional
    public void processTicker(Ticker ticker) {
        // 1. Determine start date
        Optional<WeeklyCandleData> latestWeeklyOpt = weeklyCandleDataRepository.findTopByTickerOrderByCandleDataDateDesc(ticker);
        LocalDate calculationStartDate;

        if (latestWeeklyOpt.isPresent()) {
            WeeklyCandleData latestWeekly = latestWeeklyOpt.get();
            calculationStartDate = latestWeekly.getCandleDataDate();
            log.debug("Ticker {}: Starting weekly candle computation from latest weekly candle's Monday to overwrite/update: {}", 
                    ticker.getTickerSymbol(), calculationStartDate);
        } else {
            // Default to the ticker's start date (aligned to Monday of that week)
            calculationStartDate = ticker.getTickerDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            log.debug("Ticker {}: No weekly candles found. Starting computation from ticker start date (aligned to Monday): {}", 
                    ticker.getTickerSymbol(), calculationStartDate);
        }

        // 2. Fetch daily candles starting from calculationStartDate
        List<DailyCandleData> dailyCandles = dailyCandleDataRepository.findByTickerAndCandleDataDateGreaterThanEqualOrderByCandleDataDateAsc(ticker, calculationStartDate);
        if (dailyCandles.isEmpty()) {
            log.debug("Ticker {}: No new daily candles found since {}.", ticker.getTickerSymbol(), calculationStartDate);
            return;
        }

        // 3. Group daily candles by week-start Monday
        Map<LocalDate, List<DailyCandleData>> candlesByWeek = dailyCandles.stream()
                .collect(Collectors.groupingBy(c -> c.getCandleDataDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));

        List<WeeklyCandleData> weeklyCandlesToSave = new ArrayList<>();
        LocalDate currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (Map.Entry<LocalDate, List<DailyCandleData>> entry : candlesByWeek.entrySet()) {
            LocalDate weekMonday = entry.getKey();
            List<DailyCandleData> weekCandles = entry.getValue();

            // Sort weekly daily candles by date asc to ensure correct open/close
            weekCandles.sort(Comparator.comparing(DailyCandleData::getCandleDataDate));

            DailyCandleData firstDay = weekCandles.get(0);
            DailyCandleData lastDay = weekCandles.get(weekCandles.size() - 1);

            BigDecimal open = firstDay.getPriceOpen();
            BigDecimal close = lastDay.getPriceClose();
            BigDecimal high = weekCandles.stream().map(DailyCandleData::getPriceHigh).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal low = weekCandles.stream().map(DailyCandleData::getPriceLow).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal volume = weekCandles.stream().map(DailyCandleData::getVolume).reduce(BigDecimal.ZERO, BigDecimal::add);

            // Determine if the week is completed
            // A week is finalized if the week's Monday is before the current calendar week's Monday,
            // OR if today is Saturday/Sunday (meaning the current week's trading is complete).
            boolean isFinal = weekMonday.isBefore(currentMonday) || 
                              LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY || 
                              LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY;

            // Look up existing weekly candle record to update or insert
            WeeklyCandleData weeklyCandle = weeklyCandleDataRepository.findByTickerAndCandleDataDate(ticker, weekMonday)
                    .orElse(WeeklyCandleData.builder()
                            .ticker(ticker)
                            .candleDataDate(weekMonday)
                            .build());

            weeklyCandle.setPriceOpen(open);
            weeklyCandle.setPriceHigh(high);
            weeklyCandle.setPriceLow(low);
            weeklyCandle.setPriceClose(close);
            weeklyCandle.setVolume(volume);

            weeklyCandlesToSave.add(weeklyCandle);
        }

        if (!weeklyCandlesToSave.isEmpty()) {
            weeklyCandleDataRepository.saveAll(weeklyCandlesToSave);
            log.info("Ticker {}: Saved/updated {} weekly candles.", ticker.getTickerSymbol(), weeklyCandlesToSave.size());
        }
    }
}
