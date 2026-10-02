package com.alphaflow.engine.downloaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneDownloaderTest {

  private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
  private AngelOneConfig config;
  private AngelOneClient client;
  private TickerRepository tickerRepo;
  private IntradayPriceRepository intradayRepo;
  private AngelOneDownloader downloader;

  @BeforeEach
  void setUp() {
    config = new AngelOneConfig();
    client = mock(AngelOneClient.class);
    tickerRepo = mock(TickerRepository.class);
    intradayRepo = mock(IntradayPriceRepository.class);
    downloader = new AngelOneDownloader(config, client, tickerRepo, intradayRepo);
  }

  @Test
  void testDownloadDisabledReturnsImmediately() {
    config.setEnabled(false);
    downloader.downloadIntradayPrices();

    verify(tickerRepo, never()).findByIsActiveTrueAndDataProvider(any());
    verify(client, never()).getCandleData(any(), any(), any(), any(), any());
    verify(intradayRepo, never()).saveAll(any());
  }

  @Test
  void testSyncTickerValidCandlesSaved() {
    config.setEnabled(true);
    config.setDelayMilliseconds(0);

    Ticker ticker =
        Ticker.builder().tickerSymbol("NIFTY50").dataProvider(DataProvider.ANGEL_ONE).build();

    when(tickerRepo.findByIsActiveTrueAndDataProvider(DataProvider.ANGEL_ONE))
        .thenReturn(List.of(ticker));

    OffsetDateTime latestTime = OffsetDateTime.now(IST_ZONE).minusHours(1);
    IntradayPrice existing =
        IntradayPrice.builder().ticker(ticker).priceTime(latestTime).timeframe("15M").build();

    when(intradayRepo.findTopByTickerAndTimeframeOrderByPriceTimeDesc(ticker, "15M"))
        .thenReturn(Optional.of(existing));

    OffsetDateTime newCandleTime = latestTime.plusMinutes(15);
    AngelOneCandle c1 =
        new AngelOneCandle(
            newCandleTime,
            new BigDecimal("25000.00"),
            new BigDecimal("25050.00"),
            new BigDecimal("24990.00"),
            new BigDecimal("25040.00"),
            BigDecimal.ZERO);

    when(client.getCandleData(eq("NSE"), eq("99926000"), eq("15M"), any(), any()))
        .thenReturn(List.of(c1));

    downloader.downloadIntradayPrices();

    verify(intradayRepo, times(1)).saveAll(any());
  }

  @Test
  void testSyncTickerUpToDateNoDownload() {
    config.setEnabled(true);
    config.setDelayMilliseconds(0);

    Ticker ticker =
        Ticker.builder().tickerSymbol("NIFTY50").dataProvider(DataProvider.ANGEL_ONE).build();

    when(tickerRepo.findByIsActiveTrueAndDataProvider(DataProvider.ANGEL_ONE))
        .thenReturn(List.of(ticker));

    // Saved time is in the future
    OffsetDateTime futureTime = OffsetDateTime.now(IST_ZONE).plusMinutes(15);
    IntradayPrice existing =
        IntradayPrice.builder().ticker(ticker).priceTime(futureTime).timeframe("15M").build();

    when(intradayRepo.findTopByTickerAndTimeframeOrderByPriceTimeDesc(ticker, "15M"))
        .thenReturn(Optional.of(existing));

    downloader.downloadIntradayPrices();

    verify(client, never()).getCandleData(any(), any(), any(), any(), any());
    verify(intradayRepo, never()).saveAll(any());
  }

  @Test
  void testSyncTickerEmptyCandlesReturned() {
    config.setEnabled(true);
    config.setDelayMilliseconds(0);

    Ticker ticker =
        Ticker.builder().tickerSymbol("NIFTY50").dataProvider(DataProvider.ANGEL_ONE).build();

    when(intradayRepo.findTopByTickerAndTimeframeOrderByPriceTimeDesc(ticker, "15M"))
        .thenReturn(Optional.empty());

    when(client.getCandleData(any(), any(), any(), any(), any())).thenReturn(List.of());

    downloader.syncTicker(ticker);

    verify(intradayRepo, never()).saveAll(any());
  }

  @Test
  void testSyncTickerAllPointsAlreadyExist() {
    config.setEnabled(true);
    config.setDelayMilliseconds(0);

    Ticker ticker =
        Ticker.builder().tickerSymbol("NIFTY50").dataProvider(DataProvider.ANGEL_ONE).build();

    OffsetDateTime time = OffsetDateTime.now(IST_ZONE).minusHours(1);
    IntradayPrice existing =
        IntradayPrice.builder().ticker(ticker).priceTime(time).timeframe("15M").build();

    when(intradayRepo.findTopByTickerAndTimeframeOrderByPriceTimeDesc(ticker, "15M"))
        .thenReturn(Optional.of(existing));

    // Candle older than or equal to existing
    AngelOneCandle oldCandle =
        new AngelOneCandle(
            time,
            new BigDecimal("25000.00"),
            new BigDecimal("25050.00"),
            new BigDecimal("24990.00"),
            new BigDecimal("25040.00"),
            BigDecimal.ZERO);

    when(client.getCandleData(any(), any(), any(), any(), any())).thenReturn(List.of(oldCandle));

    downloader.syncTicker(ticker);

    verify(intradayRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadInterruptedDuringDelay() {
    config.setEnabled(true);
    config.setDelayMilliseconds(2000);

    Ticker t1 =
        Ticker.builder().tickerSymbol("NIFTY50").dataProvider(DataProvider.ANGEL_ONE).build();
    Ticker t2 =
        Ticker.builder().tickerSymbol("BANKNIFTY").dataProvider(DataProvider.ANGEL_ONE).build();

    when(tickerRepo.findByIsActiveTrueAndDataProvider(DataProvider.ANGEL_ONE))
        .thenReturn(List.of(t1, t2));

    Thread mainThread = Thread.currentThread();
    new Thread(
            () -> {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              mainThread.interrupt();
            })
        .start();

    downloader.downloadIntradayPrices();
    // Interrupted status handled gracefully
  }
}
