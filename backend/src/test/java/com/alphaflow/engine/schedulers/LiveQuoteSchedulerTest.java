package com.alphaflow.engine.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.engine.strategies.SakthiIntradayStrategy;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LiveQuoteSchedulerTest {

  private AngelOneConfig angelOneConfig;
  private AngelOneClient angelOneClient;
  private TickerRepository tickerRepository;
  private LiveQuoteRepository liveQuoteRepository;
  private SakthiIntradayStrategy sakthiIntradayStrategy;
  private LiveQuoteScheduler liveQuoteScheduler;
  private Ticker niftyTicker;

  @BeforeEach
  void setUp() {
    MarketTradingCalendar.clearHolidays();
    angelOneConfig = new AngelOneConfig();
    angelOneConfig.setSessionStartTime(LocalTime.of(9, 15));
    angelOneConfig.setSessionEndTime(LocalTime.of(15, 30));

    angelOneClient = mock(AngelOneClient.class);
    tickerRepository = mock(TickerRepository.class);
    liveQuoteRepository = mock(LiveQuoteRepository.class);
    sakthiIntradayStrategy = mock(SakthiIntradayStrategy.class);

    liveQuoteScheduler =
        new LiveQuoteScheduler(
            angelOneConfig,
            angelOneClient,
            tickerRepository,
            liveQuoteRepository,
                sakthiIntradayStrategy);

    niftyTicker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .isActive(true)
            .build();
  }

  @AfterEach
  void tearDown() {
    MarketTradingCalendar.clearHolidays();
  }

  @Test
  void testRunScheduledLiveQuoteUpdateInvokesPipeline() {
    when(tickerRepository.findByIsActiveTrueAndCountryAndDataProvider(
            Country.IN, DataProvider.ANGEL_ONE))
        .thenReturn(List.of());

    liveQuoteScheduler.runScheduledLiveQuoteUpdate();
  }

  @Test
  void testRunInsertsNewLiveQuoteDuringMarketHours() {
    final ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, MarketConstants.IST_ZONE);
    OffsetDateTime quoteTimestamp =
        ZonedDateTime.of(2026, 10, 16, 9, 59, 58, 0, MarketConstants.IST_ZONE).toOffsetDateTime();

    when(tickerRepository.findByIsActiveTrueAndCountryAndDataProvider(
            Country.IN, DataProvider.ANGEL_ONE))
        .thenReturn(List.of(niftyTicker));
    when(angelOneClient.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(
            Optional.of(new AngelOneQuote(new BigDecimal("25125.75"), quoteTimestamp.toString())));
    when(liveQuoteRepository.findByTicker(niftyTicker)).thenReturn(Optional.empty());
    when(liveQuoteRepository.save(any(LiveQuote.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    liveQuoteScheduler.run(marketTime);

    ArgumentCaptor<LiveQuote> quoteCaptor = ArgumentCaptor.forClass(LiveQuote.class);
    verify(liveQuoteRepository, times(1)).save(quoteCaptor.capture());
    verify(sakthiIntradayStrategy, times(1)).execute(niftyTicker, quoteCaptor.getValue());

    LiveQuote savedQuote = quoteCaptor.getValue();
    assertEquals(niftyTicker, savedQuote.getTicker());
    assertEquals(new BigDecimal("25125.75"), savedQuote.getLastPrice());
    assertEquals(BigDecimal.ZERO, savedQuote.getVolume());
    assertEquals(quoteTimestamp, savedQuote.getQuoteTime());
    assertEquals(marketTime.toOffsetDateTime(), savedQuote.getUpdatedAt());
  }

  @Test
  void testRunUpdatesExistingLiveQuoteAndUsesFallbackTimestampWhenNonIso() {
    final ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 10, 15, 0, 0, MarketConstants.IST_ZONE);
    OffsetDateTime initialCreated =
        ZonedDateTime.of(2026, 10, 16, 9, 15, 0, 0, MarketConstants.IST_ZONE).toOffsetDateTime();

    LiveQuote existingQuote =
        LiveQuote.builder()
            .liveQuoteId(10L)
            .ticker(niftyTicker)
            .lastPrice(new BigDecimal("25000.00"))
            .volume(BigDecimal.ZERO)
            .quoteTime(initialCreated)
            .updatedAt(initialCreated)
            .build();

    when(tickerRepository.findByIsActiveTrueAndCountryAndDataProvider(
            Country.IN, DataProvider.ANGEL_ONE))
        .thenReturn(List.of(niftyTicker));
    when(angelOneClient.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(
            Optional.of(new AngelOneQuote(new BigDecimal("25200.50"), "16-Oct-2026 10:15:00")));
    when(liveQuoteRepository.findByTicker(niftyTicker)).thenReturn(Optional.of(existingQuote));
    when(liveQuoteRepository.save(any(LiveQuote.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    liveQuoteScheduler.run(marketTime);

    ArgumentCaptor<LiveQuote> quoteCaptor = ArgumentCaptor.forClass(LiveQuote.class);
    verify(liveQuoteRepository, times(1)).save(quoteCaptor.capture());

    LiveQuote updatedQuote = quoteCaptor.getValue();
    assertEquals(10L, updatedQuote.getLiveQuoteId());
    assertEquals(new BigDecimal("25200.50"), updatedQuote.getLastPrice());
    assertEquals(marketTime.toOffsetDateTime(), updatedQuote.getQuoteTime());
    assertEquals(marketTime.toOffsetDateTime(), updatedQuote.getUpdatedAt());
  }

  @Test
  void testRunSkipsAlertAndNotificationWhenAngelOneQuoteEmpty() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 11, 0, 0, 0, MarketConstants.IST_ZONE);

    when(tickerRepository.findByIsActiveTrueAndCountryAndDataProvider(
            Country.IN, DataProvider.ANGEL_ONE))
        .thenReturn(List.of(niftyTicker));
    when(angelOneClient.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(Optional.empty());

    liveQuoteScheduler.run(marketTime);

    verify(liveQuoteRepository, never()).save(any());
    verify(sakthiIntradayStrategy, never()).execute(any(), any());
  }

  @Test
  void testRunSkipsWhenMarketClosedOnWeekendOrOutsideSessionBounds() {
    // Weekend (Saturday)
    ZonedDateTime saturdayTime =
        ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, MarketConstants.IST_ZONE);
    liveQuoteScheduler.run(saturdayTime);

    // Before session start (09:14 IST)
    ZonedDateTime beforeOpenTime =
        ZonedDateTime.of(2026, 10, 16, 9, 14, 0, 0, MarketConstants.IST_ZONE);
    liveQuoteScheduler.run(beforeOpenTime);

    // After session end (15:31 IST)
    ZonedDateTime afterCloseTime =
        ZonedDateTime.of(2026, 10, 16, 15, 31, 0, 0, MarketConstants.IST_ZONE);
    liveQuoteScheduler.run(afterCloseTime);

    verify(tickerRepository, never()).findByIsActiveTrueAndCountryAndDataProvider(any(), any());
  }

  @Test
  void testRunCatchesExceptionAndResetsRunningFlag() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, MarketConstants.IST_ZONE);

    doThrow(new RuntimeException("Simulated database failure"))
        .when(tickerRepository)
        .findByIsActiveTrueAndCountryAndDataProvider(Country.IN, DataProvider.ANGEL_ONE);

    liveQuoteScheduler.run(marketTime);

    verify(tickerRepository, times(1))
        .findByIsActiveTrueAndCountryAndDataProvider(Country.IN, DataProvider.ANGEL_ONE);
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 12, 30, 0, 0, MarketConstants.IST_ZONE);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return List.of();
            })
        .when(tickerRepository)
        .findByIsActiveTrueAndCountryAndDataProvider(Country.IN, DataProvider.ANGEL_ONE);

    Thread backgroundThread = new Thread(() -> liveQuoteScheduler.run(marketTime));
    backgroundThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    // Second invocation while first is still running
    liveQuoteScheduler.run(marketTime);

    finishLatch.countDown();
    backgroundThread.join(2000);

    verify(tickerRepository, times(1))
        .findByIsActiveTrueAndCountryAndDataProvider(Country.IN, DataProvider.ANGEL_ONE);
  }
}
