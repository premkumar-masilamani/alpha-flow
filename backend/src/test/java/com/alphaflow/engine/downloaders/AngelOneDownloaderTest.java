package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneDownloaderTest {

  private AngelOneConfig config;
  private AngelOneClient client;
  private IntradayPriceRepository intradayRepo;
  private AngelOneDownloader downloader;
  private final ZoneId istZone = MarketConstants.IST_ZONE;

  @BeforeEach
  void setUp() {
    config = new AngelOneConfig();
    config.setApiKey("test-key");
    config.setClientCode("test-code");
    config.setPassword("test-pwd");
    config.setTotpKey("test-totp");
    config.setDelayMilliseconds(0);
    config.setSessionStartTime(LocalTime.of(9, 15));
    config.setSessionEndTime(LocalTime.of(15, 30));
    config.setMaxChunkDays(200);
    config.setBackfillStartDate(LocalDate.of(2015, 1, 1));

    client = mock(AngelOneClient.class);
    intradayRepo = mock(IntradayPriceRepository.class);
    downloader = new AngelOneDownloader(config, client, intradayRepo);
  }

  @Test
  void testDownloadEmptyActiveTickers() {
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of());

    int result = downloader.downloadIntradayPrices();

    assertEquals(0, result);
    verify(client, never()).getCandleData(any(), any(), any(), any(), any());
  }

  @Test
  void testDownloadTickerUpToDateSkipped() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    // Latest saved time is far in future (greater than expected completed time)
    OffsetDateTime futureTime = OffsetDateTime.now(istZone).plusDays(1);
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of(ticker, futureTime));

    int result = downloader.downloadIntradayPrices();

    assertEquals(0, result);
    verify(client, never()).getCandleData(any(), any(), any(), any(), any());
  }

  @Test
  void testDownloadTickerOutdatedSyncSuccessSingleChunk() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    // Set latest time to yesterday at 10:00 AM (guaranteed to be before expectedCompletedTime
    // regardless of time of day)
    OffsetDateTime latestTime =
        LocalDate.now(istZone).minusDays(1).atTime(10, 0).atZone(istZone).toOffsetDateTime();
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of(ticker, latestTime));

    // Candle 1: older than latestTime (filtered out by timestamp > effectiveMinTime)
    AngelOneCandle oldCandle =
        new AngelOneCandle(
            latestTime.minusMinutes(15),
            new BigDecimal("25000.00"),
            new BigDecimal("25050.00"),
            new BigDecimal("24990.00"),
            new BigDecimal("25040.00"),
            BigDecimal.ZERO);

    // Candle 2: valid new candle
    AngelOneCandle validCandle =
        new AngelOneCandle(
            latestTime.plusMinutes(15),
            new BigDecimal("25040.00"),
            new BigDecimal("25080.00"),
            new BigDecimal("25030.00"),
            new BigDecimal("25075.00"),
            BigDecimal.TEN);

    // Candle 3: far in future beyond overallEnd (filtered out by !timestamp.isAfter(overallEnd))
    AngelOneCandle futureCandle =
        new AngelOneCandle(
            OffsetDateTime.now(istZone).plusDays(5),
            new BigDecimal("25075.00"),
            new BigDecimal("25090.00"),
            new BigDecimal("25070.00"),
            new BigDecimal("25085.00"),
            BigDecimal.ONE);

    when(client.getCandleData(
            eq(MarketConstants.EXCHANGE_NSE),
            eq("99926000"),
            eq(IntradayTimeframe._15M),
            any(),
            any()))
        .thenReturn(List.of(oldCandle, validCandle, futureCandle));

    int result = downloader.downloadIntradayPrices();

    assertEquals(1, result);
    verify(intradayRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadNewTickerHistoricalBackfillMultiChunk() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    // No existing candles (latestSavedTime == null)
    Map<Ticker, OffsetDateTime> tickerMap = new LinkedHashMap<>();
    tickerMap.put(ticker, null);
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(tickerMap);

    // Configure small chunk days and recent backfill start date so it runs in 2 chunks
    config.setBackfillStartDate(LocalDate.now(istZone).minusDays(3));
    config.setMaxChunkDays(2);

    OffsetDateTime chunk1Time =
        LocalDate.now(istZone).minusDays(2).atTime(10, 0).atZone(istZone).toOffsetDateTime();
    AngelOneCandle candleChunk1 =
        new AngelOneCandle(
            chunk1Time,
            new BigDecimal("24000"),
            new BigDecimal("24100"),
            new BigDecimal("23900"),
            new BigDecimal("24050"),
            BigDecimal.ONE);

    OffsetDateTime chunk2Time =
        LocalDate.now(istZone).minusDays(1).atTime(11, 0).atZone(istZone).toOffsetDateTime();
    AngelOneCandle candleChunk2 =
        new AngelOneCandle(
            chunk2Time,
            new BigDecimal("24050"),
            new BigDecimal("24150"),
            new BigDecimal("24000"),
            new BigDecimal("24120"),
            BigDecimal.TEN);

    when(client.getCandleData(
            eq(MarketConstants.EXCHANGE_NSE),
            eq("99926000"),
            eq(IntradayTimeframe._15M),
            any(),
            any()))
        .thenReturn(List.of(candleChunk1))
        .thenReturn(List.of(candleChunk2));

    int result = downloader.downloadIntradayPrices();

    assertEquals(2, result);
    verify(client, times(2)).getCandleData(any(), any(), any(), any(), any());
    verify(intradayRepo, times(2)).saveAll(any());
  }

  @Test
  void testDownloadChunkReturnsEmptyCandles() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    OffsetDateTime latestTime =
        LocalDate.now(istZone).minusDays(1).atTime(10, 0).atZone(istZone).toOffsetDateTime();
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of(ticker, latestTime));

    when(client.getCandleData(any(), any(), any(), any(), any())).thenReturn(List.of());

    int result = downloader.downloadIntradayPrices();

    assertEquals(0, result);
    verify(intradayRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadChunkReturnsCandlesButAllFilteredOut() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    OffsetDateTime latestTime =
        LocalDate.now(istZone).minusDays(1).atTime(10, 0).atZone(istZone).toOffsetDateTime();
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of(ticker, latestTime));

    // Candle equal to latestTime -> timestamp.isAfter(latestTime) is false
    AngelOneCandle oldCandle =
        new AngelOneCandle(
            latestTime,
            new BigDecimal("25000.00"),
            new BigDecimal("25050.00"),
            new BigDecimal("24990.00"),
            new BigDecimal("25040.00"),
            BigDecimal.ZERO);

    when(client.getCandleData(any(), any(), any(), any(), any())).thenReturn(List.of(oldCandle));

    int result = downloader.downloadIntradayPrices();

    assertEquals(0, result);
    verify(intradayRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadInterruptedDuringChunkDelay() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    Map<Ticker, OffsetDateTime> tickerMap = new LinkedHashMap<>();
    tickerMap.put(ticker, null);
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(tickerMap);

    config.setBackfillStartDate(LocalDate.now(istZone).minusDays(3));
    config.setMaxChunkDays(1);
    config.setDelayMilliseconds(2000);

    when(client.getCandleData(any(), any(), any(), any(), any())).thenReturn(List.of());

    Thread mainThread = Thread.currentThread();
    new Thread(
            () -> {
              try {
                Thread.sleep(50);
              } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
              }
              mainThread.interrupt();
            })
        .start();

    int result = downloader.downloadIntradayPrices();
    assertEquals(0, result);
  }

  @Test
  void testDownloadInterruptedDuringTickerDelay() {
    Ticker ticker1 =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();
    Ticker ticker2 =
        Ticker.builder()
            .tickerSymbol("BANKNIFTY")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    Map<Ticker, OffsetDateTime> tickerMap = new LinkedHashMap<>();
    OffsetDateTime twoHoursAgo =
        LocalDate.now(istZone).minusDays(1).atTime(10, 0).atZone(istZone).toOffsetDateTime();
    tickerMap.put(ticker1, twoHoursAgo);
    tickerMap.put(ticker2, twoHoursAgo);

    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(tickerMap);

    config.setDelayMilliseconds(2000);

    when(client.getCandleData(any(), any(), any(), any(), any())).thenReturn(List.of());

    Thread mainThread = Thread.currentThread();
    new Thread(
            () -> {
              try {
                Thread.sleep(50);
              } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
              }
              mainThread.interrupt();
            })
        .start();

    int result = downloader.downloadIntradayPrices();
    assertEquals(0, result);
  }
}
