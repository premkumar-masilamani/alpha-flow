package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

/**
 * Calculates and persists {@link DailyPrice} records by aggregating 15-minute {@link IntradayPrice}
 * candles for active Indian tickers.
 */
@Component
@Slf4j
public class DailyPriceFromIntradayCalculator {

  private final TickerRepository tickerRepository;
  private final IntradayPriceRepository intradayPriceRepository;
  private final DailyPriceRepository dailyPriceRepository;

  @Autowired @Lazy private DailyPriceFromIntradayCalculator self;

  public DailyPriceFromIntradayCalculator(
      TickerRepository tickerRepository,
      IntradayPriceRepository intradayPriceRepository,
      DailyPriceRepository dailyPriceRepository) {
    this.tickerRepository = tickerRepository;
    this.intradayPriceRepository = intradayPriceRepository;
    this.dailyPriceRepository = dailyPriceRepository;
  }

  /**
   * Computes daily prices from 15-minute intraday candles for all active Indian tickers.
   */
  public void computeDailyPricesFromIntraday() {
    log.info("Starting daily price computation from 15-minute intraday prices...");
    List<Ticker> activeTickers = tickerRepository.findByIsActiveTrue();
    List<Ticker> indianTickers =
        activeTickers.stream()
            .filter(ticker -> ticker.getCountry() == Country.IN)
            .toList();

    log.info("Found {} active Indian tickers for daily price computation.", indianTickers.size());
    DailyPriceFromIntradayCalculator proxy = (self != null) ? self : this;

    for (Ticker ticker : indianTickers) {
      try {
        proxy.processTicker(ticker);
      } catch (Exception e) {
        log.error(
            "Failed to compute daily prices from intraday for ticker {}: {}",
            ticker.getTickerSymbol(),
            e.getMessage(),
            e);
      }
    }
    log.info("Completed daily price computation from 15-minute intraday prices.");
  }

  /**
   * Processes a single ticker's 15-minute intraday candles and upserts aggregated daily prices.
   *
   * @param ticker the ticker to process
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processTicker(Ticker ticker) {
    Optional<LocalDate> startDateOpt = computeStartDate(ticker);
    List<IntradayPrice> intradayCandles;
    LocalDate startDate;

    if (startDateOpt.isPresent()) {
      startDate = startDateOpt.get();
      OffsetDateTime startDateTime = startDate.atStartOfDay(MarketConfig.IST_ZONE).toOffsetDateTime();
      intradayCandles =
          intradayPriceRepository
              .findByTickerAndTimeframeAndPriceTimeGreaterThanEqualOrderByPriceTimeAsc(
                  ticker, IntradayTimeframe._15M.getValue(), startDateTime);
    } else {
      intradayCandles =
          intradayPriceRepository.findByTickerAndTimeframeOrderByPriceTimeAsc(
              ticker, IntradayTimeframe._15M.getValue());
      if (!intradayCandles.isEmpty()) {
        startDate =
            intradayCandles.getFirst().getPriceTime().atZoneSameInstant(MarketConfig.IST_ZONE).toLocalDate();
      } else {
        startDate = null;
      }
    }

    if (intradayCandles.isEmpty()) {
      log.debug(
          "Ticker {}: No 15-minute intraday prices found to aggregate.", ticker.getTickerSymbol());
      return;
    }

    List<DailyPrice> computedBars = aggregateIntradayToDaily(ticker, intradayCandles);
    saveDailyBars(ticker, computedBars, startDate);
  }

  private Optional<LocalDate> computeStartDate(Ticker ticker) {
    Optional<DailyPrice> latestDailyOpt =
        dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker);
    if (latestDailyOpt.isPresent()) {
      LocalDate calculationStartDate = latestDailyOpt.get().getPriceDate();
      log.debug(
          "Ticker {}: Starting daily price aggregation from latest recorded date: {}",
          ticker.getTickerSymbol(),
          calculationStartDate);
      return Optional.of(calculationStartDate);
    } else {
      log.debug(
          "Ticker {}: No existing daily prices found. Aggregating all available intraday prices.",
          ticker.getTickerSymbol());
      return Optional.empty();
    }
  }

  private List<DailyPrice> aggregateIntradayToDaily(
      Ticker ticker, List<IntradayPrice> intradayCandles) {
    Map<LocalDate, List<IntradayPrice>> candlesByDate =
        intradayCandles.stream()
            .collect(
                Collectors.groupingBy(
                    candle ->
                        candle
                            .getPriceTime()
                            .atZoneSameInstant(MarketConfig.IST_ZONE)
                            .toLocalDate()));

    List<DailyPrice> aggregatedList = new ArrayList<>(candlesByDate.size());

    for (Map.Entry<LocalDate, List<IntradayPrice>> entry : candlesByDate.entrySet()) {
      LocalDate tradeDate = entry.getKey();
      List<IntradayPrice> dailyCandles = entry.getValue();
      dailyCandles.sort(Comparator.comparing(IntradayPrice::getPriceTime));

      IntradayPrice firstCandle = dailyCandles.getFirst();
      IntradayPrice lastCandle = dailyCandles.getLast();
      BigDecimal priceOpen = firstCandle.getPriceOpen();
      BigDecimal priceClose = lastCandle.getPriceClose();

      BigDecimal priceHigh =
          dailyCandles.stream()
              .map(IntradayPrice::getPriceHigh)
              .max(BigDecimal::compareTo)
              .orElse(firstCandle.getPriceHigh());

      BigDecimal priceLow =
          dailyCandles.stream()
              .map(IntradayPrice::getPriceLow)
              .min(BigDecimal::compareTo)
              .orElse(firstCandle.getPriceLow());

      BigDecimal volume =
          dailyCandles.stream()
              .map(IntradayPrice::getVolume)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      DailyPrice dailyPrice =
          DailyPrice.builder()
              .ticker(ticker)
              .priceDate(tradeDate)
              .priceOpen(priceOpen)
              .priceHigh(priceHigh)
              .priceLow(priceLow)
              .priceClose(priceClose)
              .volume(volume)
              .build();

      aggregatedList.add(dailyPrice);
    }

    aggregatedList.sort(Comparator.comparing(DailyPrice::getPriceDate));
    return aggregatedList;
  }

  private void saveDailyBars(Ticker ticker, List<DailyPrice> computedBars, LocalDate startDate) {
    Map<LocalDate, DailyPrice> existingDailyByDate =
        dailyPriceRepository
            .findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                ticker, startDate)
            .stream()
            .collect(Collectors.toMap(DailyPrice::getPriceDate, dp -> dp));

    List<DailyPrice> dailyPricesToSave = new ArrayList<>(computedBars.size());
    for (DailyPrice computed : computedBars) {
      DailyPrice existing = existingDailyByDate.get(computed.getPriceDate());
      if (existing != null) {
        existing.setPriceOpen(computed.getPriceOpen());
        existing.setPriceHigh(computed.getPriceHigh());
        existing.setPriceLow(computed.getPriceLow());
        existing.setPriceClose(computed.getPriceClose());
        existing.setVolume(computed.getVolume());
        dailyPricesToSave.add(existing);
      } else {
        dailyPricesToSave.add(computed);
      }
    }

    dailyPriceRepository.saveAll(dailyPricesToSave);
    log.info(
        "{}: Saved/updated {} daily prices from 15-minute intraday prices.",
        ticker.getTickerSymbol(),
        dailyPricesToSave.size());
  }
}
