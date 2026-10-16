package com.alphaflow.engine.strategies;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.exceptions.NotificationException;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SakthIntradayStrategyTest {

  private DailyPriceRepository dailyPriceRepository;
  private NotificationService notificationService;
  private SakthIntradayStrategy strategy;
  private Ticker ticker;
  private OffsetDateTime baseQuoteTime;

  @BeforeEach
  void setUp() {
    dailyPriceRepository = mock(DailyPriceRepository.class);
    notificationService = mock(NotificationService.class);
    strategy = new SakthIntradayStrategy(dailyPriceRepository, notificationService);

    ticker = Ticker.builder().tickerId(101L).tickerSymbol("NIFTY50").build();
    baseQuoteTime =
        LocalDate.of(2026, 10, 16)
            .atTime(10, 30, 0)
            .atZone(MarketConstants.IST_ZONE)
            .toOffsetDateTime();
  }

  private DailyPrice createPreviousDailyPrice(
      LocalDate date, BigDecimal priceHigh, BigDecimal priceLow) {
    return DailyPrice.builder()
        .priceDate(date)
        .priceOpen(new BigDecimal("24100.00"))
        .priceHigh(priceHigh)
        .priceLow(priceLow)
        .priceClose(new BigDecimal("24150.00"))
        .volume(new BigDecimal("100000"))
        .build();
  }

  private LiveQuote createLiveQuote(BigDecimal lastPrice, OffsetDateTime quoteTime) {
    return LiveQuote.builder().ticker(ticker).lastPrice(lastPrice).quoteTime(quoteTime).build();
  }

  @Test
  void testNoPriorDailyPrices_SkipsEvaluationGracefully() {
    LiveQuote quote = createLiveQuote(new BigDecimal("23950.00"), baseQuoteTime);
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> strategy.evaluateAndNotify(ticker, quote));
    verify(notificationService, never()).sendNotification(anyString());
  }

  @Test
  void testPriceWithinRange_NoNotificationsTriggered() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    LiveQuote quote = createLiveQuote(new BigDecimal("24250.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote);

    verify(notificationService, never()).sendNotification(anyString());
  }

  @Test
  void testBearishPdlBreach_TriggersNotification1() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    LiveQuote quote = createLiveQuote(new BigDecimal("23980.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(1)).sendNotification(captor.capture());
    String sentMessage = captor.getValue();
    assertTrue(sentMessage.contains("BEARISH ALERT"));
    assertTrue(sentMessage.contains("NIFTY50"));
    assertTrue(sentMessage.contains("23980"));
    assertTrue(sentMessage.contains("24000"));
  }

  @Test
  void testBearishLowerLow_UpdatesLowestPriceWithoutNotification2() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDL (24000) at 23980 -> Notification 1
    LiveQuote quote1 = createLiveQuote(new BigDecimal("23980.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote1);
    verify(notificationService, times(1)).sendNotification(anyString());

    // Second tick makes a lower low at 23920 -> updates lowest price, NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23920.00"), baseQuoteTime.plusMinutes(1));
    strategy.evaluateAndNotify(ticker, quote2);
    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testBearishSmallBounce_NoNotification2() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDL at 23920
    LiveQuote quote1 = createLiveQuote(new BigDecimal("23920.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote1);

    // Bounce of only 30 points (23950 < 23920 + 70) -> NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23950.00"), baseQuoteTime.plusMinutes(1));
    strategy.evaluateAndNotify(ticker, quote2);

    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testBearishBounce_TriggersNotification2_AndIgnoresSubsequentTicks() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick: Breach PDL at 23920
    LiveQuote quote1 = createLiveQuote(new BigDecimal("23920.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote1);

    // Second tick: Bounce 70 points to 23990 (>= 23920 + 70) -> Notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23990.00"), baseQuoteTime.plusMinutes(1));
    strategy.evaluateAndNotify(ticker, quote2);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());
    String secondMessage = captor.getAllValues().get(1);
    assertTrue(secondMessage.contains("BULLISH REVERSAL"));
    assertTrue(secondMessage.contains("23990"));
    assertTrue(secondMessage.contains("23920"));
    assertTrue(secondMessage.contains("+70 points from low"));

    // Third tick: Further fluctuations -> NO third notification
    LiveQuote quote3 = createLiveQuote(new BigDecimal("24050.00"), baseQuoteTime.plusMinutes(2));
    strategy.evaluateAndNotify(ticker, quote3);
    verify(notificationService, times(2)).sendNotification(anyString());
  }

  @Test
  void testBullishPdhBreach_TriggersNotification1() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    LiveQuote quote = createLiveQuote(new BigDecimal("24510.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(1)).sendNotification(captor.capture());
    String sentMessage = captor.getValue();
    assertTrue(sentMessage.contains("BULLISH ALERT"));
    assertTrue(sentMessage.contains("NIFTY50"));
    assertTrue(sentMessage.contains("24510"));
    assertTrue(sentMessage.contains("24500"));
  }

  @Test
  void testBullishHigherHigh_UpdatesHighestPriceWithoutNotification2() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDH (24500) at 24520 -> Notification 1
    LiveQuote quote1 = createLiveQuote(new BigDecimal("24520.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote1);
    verify(notificationService, times(1)).sendNotification(anyString());

    // Second tick makes higher high at 24580 -> updates highest price, NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("24580.00"), baseQuoteTime.plusMinutes(1));
    strategy.evaluateAndNotify(ticker, quote2);
    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testBullishSmallPullback_NoNotification2() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDH at 24580
    LiveQuote quote1 = createLiveQuote(new BigDecimal("24580.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote1);

    // Pullback of only 30 points (24550 > 24580 - 70) -> NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("24550.00"), baseQuoteTime.plusMinutes(1));
    strategy.evaluateAndNotify(ticker, quote2);

    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testBullishPullback_TriggersNotification2_AndIgnoresSubsequentTicks() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDH at 24580 -> Notification 1
    LiveQuote quote1 = createLiveQuote(new BigDecimal("24580.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, quote1);

    // Second tick: Pullback of 70 points to 24510 (<= 24580 - 70) -> Notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("24510.00"), baseQuoteTime.plusMinutes(1));
    strategy.evaluateAndNotify(ticker, quote2);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());
    String secondMessage = captor.getAllValues().get(1);
    assertTrue(secondMessage.contains("BEARISH REVERSAL"));
    assertTrue(secondMessage.contains("24510"));
    assertTrue(secondMessage.contains("24580"));
    assertTrue(secondMessage.contains("-70 points from high"));

    // Third tick: Further fluctuations -> NO third notification
    LiveQuote quote3 = createLiveQuote(new BigDecimal("24450.00"), baseQuoteTime.plusMinutes(2));
    strategy.evaluateAndNotify(ticker, quote3);
    verify(notificationService, times(2)).sendNotification(anyString());
  }

  @Test
  void testNotificationException_CaughtAndLoggedWithoutThrowing() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    doThrow(new NotificationException("Network timeout", new RuntimeException("Timeout")))
        .when(notificationService)
        .sendNotification(anyString());

    LiveQuote quote = createLiveQuote(new BigDecimal("23950.00"), baseQuoteTime);

    assertDoesNotThrow(() -> strategy.evaluateAndNotify(ticker, quote));
    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testNewTradingDay_ResetsStrategyState() {
    DailyPrice day1PreviousPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 14), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    DailyPrice day2PreviousPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24300.00"), new BigDecimal("23800.00"));

    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(day1PreviousPrice))
        .thenReturn(List.of(day2PreviousPrice));

    // Day 1: PDL breach at 23980
    LiveQuote day1Quote = createLiveQuote(new BigDecimal("23980.00"), baseQuoteTime);
    strategy.evaluateAndNotify(ticker, day1Quote);
    verify(notificationService, times(1)).sendNotification(anyString());

    // Day 2 (date rollover): Quote on new day breaches new PDL at 23750 (< 23800)
    OffsetDateTime nextDayTime = baseQuoteTime.plusDays(1);
    LiveQuote day2Quote = createLiveQuote(new BigDecimal("23750.00"), nextDayTime);
    strategy.evaluateAndNotify(ticker, day2Quote);

    // Notification 1 fires again for the new day because state was reset
    verify(notificationService, times(2)).sendNotification(anyString());
  }
}
