package com.alphaflow.api.services;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class IntradayPriceService {

  private final IntradayPriceRepository intradayPriceRepository;
  private final LiveQuoteRepository liveQuoteRepository;
  private final AngelOneDownloader angelOneDownloader;
  private final AngelOneConfig angelOneConfig;

  public IntradayPriceService(
      IntradayPriceRepository intradayPriceRepository,
      LiveQuoteRepository liveQuoteRepository,
      AngelOneDownloader angelOneDownloader,
      AngelOneConfig angelOneConfig) {
    this.intradayPriceRepository = intradayPriceRepository;
    this.liveQuoteRepository = liveQuoteRepository;
    this.angelOneDownloader = angelOneDownloader;
    this.angelOneConfig = angelOneConfig;
  }

  public List<OhlcvDto> getIntradayPrice(
      Ticker ticker, IntradayTimeframe timeframe, LocalDate endDate, int page, int size) {
    ZoneId marketZone = ticker.getCountry().getZoneId();
    OffsetDateTime endDateTime =
        endDate.atTime(LocalTime.MAX).atZone(marketZone).toOffsetDateTime();
    List<IntradayPrice> persistedPrices =
        intradayPriceRepository.findLatestByTickerAndTimeframeAndEndDate(
            ticker, timeframe.getValue(), endDateTime, PageRequest.of(page, size));

    if (page == 0 && ticker.getDataProvider() == DataProvider.ANGEL_ONE) {
      OffsetDateTime expectedCompletedTime =
          MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
              ZonedDateTime.now(marketZone),
              angelOneConfig.getSessionStartTime(),
              angelOneConfig.getSessionEndTime(),
              15,
              ticker.getCountry());

      if (!persistedPrices.isEmpty()
          && !persistedPrices.getFirst().getPriceTime().isBefore(expectedCompletedTime)) {
        log.debug(
            "Intraday prices for {} are already up to date.", ticker.getTickerSymbol());
      } else {
        log.info(
            "Intraday prices for {} are missing or outdated. Syncing now...",
            ticker.getTickerSymbol());
        try {
          angelOneDownloader.downloadAndPersistIntradayPrices();
          persistedPrices =
              intradayPriceRepository.findLatestByTickerAndTimeframeAndEndDate(
                  ticker, timeframe.getValue(), endDateTime, PageRequest.of(page, size));
        } catch (Exception exception) {
          log.error(
              "Failed to sync intraday prices for {}: {}",
              ticker.getTickerSymbol(),
              exception.getMessage());
        }
      }
    }

    List<OhlcvDto> candles =
        new ArrayList<>(
            persistedPrices.stream()
                .sorted(Comparator.comparing(IntradayPrice::getPriceTime))
                .map(OhlcvMapper::toDto)
                .toList());

    mergeLiveQuote(ticker, marketZone, candles);
    return candles;
  }

  private void mergeLiveQuote(Ticker ticker, ZoneId marketZone, List<OhlcvDto> candles) {
    Optional<LiveQuote> liveQuoteOptional = liveQuoteRepository.findByTicker(ticker);
    if (liveQuoteOptional.isPresent()) {
      LiveQuote liveQuote = liveQuoteOptional.get();
      if (candles.isEmpty() || liveQuote.getQuoteTime().isAfter(candles.getLast().priceDate())) {
        ZonedDateTime quoteZoned = liveQuote.getQuoteTime().atZoneSameInstant(marketZone);
        int flooredMinute = (quoteZoned.getMinute() / 15) * 15;
        OffsetDateTime bucketTime =
            quoteZoned.withMinute(flooredMinute).withSecond(0).withNano(0).toOffsetDateTime();
        BigDecimal quotePrice = liveQuote.getLastPrice();
        if (candles.isEmpty() || bucketTime.isAfter(candles.getLast().priceDate())) {
          candles.add(
              OhlcvDto.builder()
                  .priceDate(bucketTime)
                  .priceOpen(quotePrice)
                  .priceHigh(quotePrice)
                  .priceLow(quotePrice)
                  .priceClose(quotePrice)
                  .volume(liveQuote.getVolume())
                  .build());
        } else {
          OhlcvDto latestCandle = candles.getLast();
          candles.set(
              candles.size() - 1,
              OhlcvDto.builder()
                  .priceDate(latestCandle.priceDate())
                  .priceOpen(latestCandle.priceOpen())
                  .priceHigh(latestCandle.priceHigh().max(quotePrice))
                  .priceLow(latestCandle.priceLow().min(quotePrice))
                  .priceClose(quotePrice)
                  .volume(latestCandle.volume())
                  .build());
        }
      } else {
        log.debug(
            "Live quote time {} for ticker {} is not after latest candle time {}",
            liveQuote.getQuoteTime(),
            ticker.getTickerSymbol(),
            candles.getLast().priceDate());
      }
    } else {
      log.debug("No live quote available to merge for ticker {}", ticker.getTickerSymbol());
    }
  }
}
