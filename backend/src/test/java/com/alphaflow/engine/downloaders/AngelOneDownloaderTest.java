package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneDownloaderTest {

  private AngelOneConfig config;
  private AngelOneClient client;
  private IntradayPriceRepository intradayRepo;
  private TickerRepository tickerRepo;
  private LiveQuoteRepository liveQuoteRepo;
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
    config.setBackfillStartDate(LocalDate.of(2015, 1, 1));

    client = mock(AngelOneClient.class);
    intradayRepo = mock(IntradayPriceRepository.class);
    tickerRepo = mock(TickerRepository.class);
    liveQuoteRepo = mock(LiveQuoteRepository.class);
    downloader =
        new AngelOneDownloader(config, client, intradayRepo, tickerRepo, liveQuoteRepo);
  }

  @Test
  void testDownloadEmptyActiveTickers() {
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of());

    int result = downloader.downloadIntradayPrices();

    assertEquals(0, result);
    verify(client, never()).fetch15mCandles(any(), any(), any(), any());
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
    verify(client, never()).fetch15mCandles(any(), any(), any(), any());
  }

  @Test
  void testDownloadTickerOutdatedSyncSuccessSingleChunk() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    OffsetDateTime expectedCompletedTime =
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            ZonedDateTime.now(istZone),
            config.getSessionStartTime(),
            config.getSessionEndTime(),
            15,
            Country.IN);
    OffsetDateTime latestTime = expectedCompletedTime.minusMinutes(30);
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
            expectedCompletedTime.plusDays(5),
            new BigDecimal("25075.00"),
            new BigDecimal("25090.00"),
            new BigDecimal("25070.00"),
            new BigDecimal("25085.00"),
            BigDecimal.ONE);

    when(client.fetch15mCandles(
            eq(MarketConstants.EXCHANGE_NSE),
            eq("99926000"),
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

    OffsetDateTime expectedCompletedTime =
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            ZonedDateTime.now(istZone),
            config.getSessionStartTime(),
            config.getSessionEndTime(),
            15,
            Country.IN);

    config.setBackfillStartDate(expectedCompletedTime.toLocalDate().minusDays(3));

    OffsetDateTime chunk1Time =
        config.getBackfillStartDate().atTime(10, 0).atZone(istZone).toOffsetDateTime();
    AngelOneCandle candleChunk1 =
        new AngelOneCandle(
            chunk1Time,
            new BigDecimal("24000"),
            new BigDecimal("24100"),
            new BigDecimal("23900"),
            new BigDecimal("24050"),
            BigDecimal.ONE);

    OffsetDateTime chunk2Time = expectedCompletedTime.minusMinutes(15);
    AngelOneCandle candleChunk2 =
        new AngelOneCandle(
            chunk2Time,
            new BigDecimal("24050"),
            new BigDecimal("24150"),
            new BigDecimal("24000"),
            new BigDecimal("24120"),
            BigDecimal.TEN);

    when(client.fetch15mCandles(
            eq(MarketConstants.EXCHANGE_NSE),
            eq("99926000"),
            any(),
            any()))
        .thenReturn(List.of(candleChunk1, candleChunk2));

    int result = downloader.downloadIntradayPrices();

    assertEquals(2, result);
    verify(client, times(1)).fetch15mCandles(any(), any(), any(), any());
    verify(intradayRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadDeduplicatesDuplicateCandlesInBatch() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    OffsetDateTime expectedCompletedTime =
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            ZonedDateTime.now(istZone),
            config.getSessionStartTime(),
            config.getSessionEndTime(),
            15,
            Country.IN);
    OffsetDateTime latestTime = expectedCompletedTime.minusMinutes(30);
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of(ticker, latestTime));

    OffsetDateTime candleTime = expectedCompletedTime.minusMinutes(15);
    AngelOneCandle candle1 =
        new AngelOneCandle(
            candleTime,
            new BigDecimal("24000"),
            new BigDecimal("24100"),
            new BigDecimal("23900"),
            new BigDecimal("24050"),
            BigDecimal.ONE);
    AngelOneCandle candleDuplicate =
        new AngelOneCandle(
            candleTime,
            new BigDecimal("24000"),
            new BigDecimal("24100"),
            new BigDecimal("23900"),
            new BigDecimal("24050"),
            BigDecimal.ONE);

    when(client.fetch15mCandles(
            eq(MarketConstants.EXCHANGE_NSE),
            eq("99926000"),
            any(),
            any()))
        .thenReturn(List.of(candle1, candleDuplicate));

    int result = downloader.downloadIntradayPrices();

    assertEquals(1, result);
    verify(intradayRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadChunkReturnsEmptyCandles() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    OffsetDateTime expectedCompletedTime =
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            ZonedDateTime.now(istZone),
            config.getSessionStartTime(),
            config.getSessionEndTime(),
            15,
            Country.IN);
    OffsetDateTime latestTime = expectedCompletedTime.minusMinutes(30);
    when(intradayRepo.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue()))
        .thenReturn(Map.of(ticker, latestTime));

    when(client.fetch15mCandles(any(), any(), any(), any())).thenReturn(List.of());

    int result = downloader.downloadIntradayPrices();

    assertEquals(0, result);
    verify(client, times(1)).fetch15mCandles(any(), any(), any(), any());
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

    OffsetDateTime expectedCompletedTime =
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            ZonedDateTime.now(istZone),
            config.getSessionStartTime(),
            config.getSessionEndTime(),
            15,
            Country.IN);
    OffsetDateTime latestTime = expectedCompletedTime.minusMinutes(30);
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

    when(client.fetch15mCandles(any(), any(), any(), any())).thenReturn(List.of(oldCandle));

    int result = downloader.downloadIntradayPrices();

    assertEquals(0, result);
    verify(client, times(1)).fetch15mCandles(any(), any(), any(), any());
    verify(intradayRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadLiveQuotesSuccessInsertAndSkipMissing() {
    Ticker activeTicker1 =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .isActive(true)
            .build();
    Ticker activeTicker2 =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("BANKNIFTY")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .isActive(true)
            .build();

    OffsetDateTime currentTimestamp = OffsetDateTime.now(MarketConstants.IST_ZONE);

    when(tickerRepo.findByIsActiveTrueAndCountryAndDataProvider(Country.IN, DataProvider.ANGEL_ONE))
        .thenReturn(List.of(activeTicker1, activeTicker2));

    when(client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(
            Optional.of(new AngelOneQuote(new BigDecimal("25125.75"), "2026-10-16T10:00:00+05:30")));
    when(client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926009"))
        .thenReturn(Optional.empty());

    when(liveQuoteRepo.findByTicker(activeTicker1)).thenReturn(Optional.empty());
    when(liveQuoteRepo.save(any(LiveQuote.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<LiveQuote> results = downloader.downloadLiveQuotes(currentTimestamp);

    assertEquals(1, results.size());
    LiveQuote saved = results.getFirst();
    assertEquals(activeTicker1, saved.getTicker());
    assertEquals(new BigDecimal("25125.75"), saved.getLastPrice());
    assertEquals(BigDecimal.ZERO, saved.getVolume());
    verify(liveQuoteRepo, times(1)).save(any(LiveQuote.class));
  }

  @Test
  void testDownloadLiveQuoteForTickerUpdatesExisting() {
    Ticker ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .isActive(true)
            .build();

    OffsetDateTime currentTimestamp = OffsetDateTime.now(MarketConstants.IST_ZONE);
    LiveQuote existing =
        LiveQuote.builder()
            .liveQuoteId(10L)
            .ticker(ticker)
            .lastPrice(new BigDecimal("24900.00"))
            .quoteTime(currentTimestamp.minusMinutes(1))
            .build();

    when(client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(
            Optional.of(new AngelOneQuote(new BigDecimal("25000.00"), "2026-10-16T10:01:00+05:30")));
    when(liveQuoteRepo.findByTicker(ticker)).thenReturn(Optional.of(existing));
    when(liveQuoteRepo.save(any(LiveQuote.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<LiveQuote> updatedOpt = downloader.downloadLiveQuoteForTicker(ticker, currentTimestamp);

    assertTrue(updatedOpt.isPresent());
    LiveQuote updated = updatedOpt.get();
    assertEquals(10L, updated.getLiveQuoteId());
    assertEquals(new BigDecimal("25000.00"), updated.getLastPrice());
    assertSame(ticker, updated.getTicker());
    verify(liveQuoteRepo, times(1)).save(existing);
  }

  @Test
  void testDownloadLiveQuoteForTickerEmptyQuote() {
    Ticker ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .isActive(true)
            .build();

    when(client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(Optional.empty());

    Optional<LiveQuote> result =
        downloader.downloadLiveQuoteForTicker(ticker, OffsetDateTime.now(MarketConstants.IST_ZONE));

    assertFalse(result.isPresent());
    verify(liveQuoteRepo, never()).save(any());
  }
}
