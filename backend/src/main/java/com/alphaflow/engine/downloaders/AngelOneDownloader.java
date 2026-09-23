package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneDownloader {

  public static final String TIMEFRAME_15M = "FIFTEEN_MINUTE";
  private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final AngelOneConfig config;
  private final AngelOneClient client;
  private final TickerRepository tickerRepository;
  private final IntradayPriceRepository intradayPriceRepository;

  public AngelOneDownloader(
      AngelOneConfig config,
      AngelOneClient client,
      TickerRepository tickerRepository,
      IntradayPriceRepository intradayPriceRepository) {
    this.config = config;
    this.client = client;
    this.tickerRepository = tickerRepository;
    this.intradayPriceRepository = intradayPriceRepository;
  }

  public void downloadIntradayPrices() {
    if (!config.isEnabled()) {
      log.info("Angel One downloader is disabled (alphaflow.angelone.enabled=false).");
      return;
    }

    log.info("Starting Angel One 15-minute intraday data download process...");

    List<Ticker> activeTickers =
        tickerRepository.findByIsActiveTrueAndDataProvider(DataProvider.ANGEL_ONE);
    log.info("Found {} active Angel One tickers to sync.", activeTickers.size());

    for (Ticker ticker : activeTickers) {
      syncTicker(ticker);
      try {
        Thread.sleep(config.getDelayMilliseconds());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Angel One download process interrupted during delay.");
        break;
      }
    }

    log.info("All Angel One 15-minute downloads completed!");
  }

  public void syncTicker(Ticker ticker) {
    Optional<IntradayPrice> latestSaved =
        intradayPriceRepository.findTopByTickerAndTimeframeOrderByPriceTimeDesc(
            ticker, TIMEFRAME_15M);

    OffsetDateTime now = OffsetDateTime.now(IST_ZONE);
    OffsetDateTime fromTime =
        latestSaved
            .map(IntradayPrice::getPriceTime)
            .orElseGet(
                () ->
                    now.minusDays(config.getInitialLookbackDays())
                        .withHour(9)
                        .withMinute(15)
                        .withSecond(0)
                        .withNano(0));

    if (!fromTime.isBefore(now)) {
      log.info("{}: up to date (last sync: {}).", ticker.getTickerSymbol(), fromTime);
      return;
    }

    String fromDateStr = fromTime.format(DATE_TIME_FORMATTER);
    String toDateStr = now.format(DATE_TIME_FORMATTER);

    String exchange = "NSE";
    String token = AngelOneClient.resolveToken(ticker.getTickerSymbol());

    log.info(
        "Syncing 15m candles for {} (token: {}) from {} to {}",
        ticker.getTickerSymbol(),
        token,
        fromDateStr,
        toDateStr);

    List<AngelOneCandle> candles =
        client.getCandleData(exchange, token, TIMEFRAME_15M, fromDateStr, toDateStr);

    if (candles.isEmpty()) {
      log.info("No candle data returned for {}", ticker.getTickerSymbol());
      return;
    }

    OffsetDateTime minTime =
        latestSaved.map(IntradayPrice::getPriceTime).orElse(OffsetDateTime.MIN);
    List<IntradayPrice> toSave = new ArrayList<>();

    for (AngelOneCandle candle : candles) {
      if (candle.timestamp().isAfter(minTime)) {
        toSave.add(
            IntradayPrice.builder()
                .ticker(ticker)
                .timeframe(TIMEFRAME_15M)
                .priceTime(candle.timestamp())
                .priceOpen(candle.open())
                .priceHigh(candle.high())
                .priceLow(candle.low())
                .priceClose(candle.close())
                .volume(candle.volume())
                .build());
      }
    }

    if (!toSave.isEmpty()) {
      intradayPriceRepository.saveAll(toSave);
      log.info(
          "Successfully synced {} new 15-minute candles for {}",
          toSave.size(),
          ticker.getTickerSymbol());
    } else {
      log.info(
          "No new 15-minute data points to save for ticker {} (all downloaded points already exist).",
          ticker.getTickerSymbol());
    }
  }
}
