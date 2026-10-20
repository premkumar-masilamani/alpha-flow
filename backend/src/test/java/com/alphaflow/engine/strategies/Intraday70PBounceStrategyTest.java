package com.alphaflow.engine.strategies;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.exceptions.NotificationException;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.engine.services.NotificationTemplateEngine;
import com.alphaflow.engine.strategies.options.NiftyOptionSelectorService;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

class Intraday70PBounceStrategyTest {

  private DailyPriceRepository dailyPriceRepository;
  private NotificationService notificationService;
  private NiftyOptionSelectorService niftyOptionSelectorService;
  private Intraday70PBounceStrategy strategy;
  private Ticker ticker;
  private OffsetDateTime baseQuoteTime;

  @BeforeEach
  void setUp() {
    dailyPriceRepository = mock(DailyPriceRepository.class);
    notificationService = mock(NotificationService.class);
    niftyOptionSelectorService = mock(NiftyOptionSelectorService.class);
    NotificationTemplateEngine notificationTemplateEngine = new NotificationTemplateEngine();
    strategy =
        new Intraday70PBounceStrategy(
            dailyPriceRepository,
            notificationService,
            niftyOptionSelectorService,
            notificationTemplateEngine);

    ticker =
        Ticker.builder()
            .tickerId(101L)
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .build();
    baseQuoteTime =
        LocalDate.of(2026, 10, 16)
            .atTime(10, 30, 0)
            .atZone(MarketConfig.IST_ZONE)
            .toOffsetDateTime();
  }

  @AfterEach
  void tearDown() {
    MarketTradingCalendar.clearHolidays();
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
  void testNoPriorDailyPrices_SkipsEvaluationGracefully_AndDoesNotQueryDbAgainForSameSession() {
    LiveQuote quote1 = createLiveQuote(new BigDecimal("23950.00"), baseQuoteTime);
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23900.00"), baseQuoteTime.plusMinutes(1));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    assertDoesNotThrow(() -> strategy.execute(ticker, quote1));
    assertDoesNotThrow(() -> strategy.execute(ticker, quote2));
    verify(dailyPriceRepository, times(1)).findLatestByTickerAndEndDate(any(), any(), any());
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
    strategy.execute(ticker, quote);

    verify(notificationService, never()).sendNotification(anyString());
  }

  @Test
  void testBearishPdlBreach_TriggersNotification1WithoutIcons() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    LiveQuote quote = createLiveQuote(new BigDecimal("23980.00"), baseQuoteTime);
    strategy.execute(ticker, quote);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(1)).sendNotification(captor.capture());
    String sentMessage = captor.getValue();
    assertTrue(sentMessage.contains("BEARISH ALERT"));
    assertTrue(sentMessage.contains("NIFTY50"));
    assertTrue(sentMessage.contains("23980"));
    assertTrue(sentMessage.contains("24000"));
    assertFalse(sentMessage.contains("🚨"));
    assertFalse(sentMessage.contains("🟢"));
    assertFalse(sentMessage.contains("🔴"));
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
    strategy.execute(ticker, quote1);
    verify(notificationService, times(1)).sendNotification(anyString());

    // Second tick makes a lower low at 23920 -> updates lowest price, NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23920.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);
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
    strategy.execute(ticker, quote1);

    // Bounce of only 30 points (23950 < 23920 + 70) -> NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23950.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);

    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testBearishBounce_TriggersNotification2_AndIgnoresSubsequentTicks_AndCallsDbOnce() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick: Breach PDL at 23920
    LiveQuote quote1 = createLiveQuote(new BigDecimal("23920.00"), baseQuoteTime);
    strategy.execute(ticker, quote1);

    // Second tick: Bounce 75 points to 23995 (>= 23920 + 70) -> Notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23995.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());
    String secondMessage = captor.getAllValues().get(1);
    assertTrue(secondMessage.contains("BULLISH REVERSAL"));
    assertTrue(secondMessage.contains("*Spot Price:* 23995"));
    assertTrue(secondMessage.contains("23920"));
    assertTrue(secondMessage.contains("75 points from 23920"));
    assertFalse(secondMessage.contains("🚨"));
    assertFalse(secondMessage.contains("🟢"));
    assertFalse(secondMessage.contains("🔴"));

    // Third tick: Further fluctuations -> NO third notification
    LiveQuote quote3 = createLiveQuote(new BigDecimal("24050.00"), baseQuoteTime.plusMinutes(2));
    strategy.execute(ticker, quote3);
    verify(notificationService, times(2)).sendNotification(anyString());

    // DB call should be executed ONLY ONCE for the session date
    verify(dailyPriceRepository, times(1)).findLatestByTickerAndEndDate(any(), any(), any());
  }

  @Test
  void testBullishPdhBreach_TriggersNotification1WithoutIcons() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    LiveQuote quote = createLiveQuote(new BigDecimal("24510.00"), baseQuoteTime);
    strategy.execute(ticker, quote);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(1)).sendNotification(captor.capture());
    String sentMessage = captor.getValue();
    assertTrue(sentMessage.contains("BULLISH ALERT"));
    assertTrue(sentMessage.contains("NIFTY50"));
    assertTrue(sentMessage.contains("24510"));
    assertTrue(sentMessage.contains("24500"));
    assertFalse(sentMessage.contains("🚨"));
    assertFalse(sentMessage.contains("🟢"));
    assertFalse(sentMessage.contains("🔴"));
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
    strategy.execute(ticker, quote1);
    verify(notificationService, times(1)).sendNotification(anyString());

    // Second tick makes higher high at 24580 -> updates highest price, NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("24580.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);
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
    strategy.execute(ticker, quote1);

    // Pullback of only 30 points (24550 > 24580 - 70) -> NO notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("24550.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);

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
    strategy.execute(ticker, quote1);

    // Second tick: Pullback of 75 points to 24505 (<= 24580 - 70) -> Notification 2
    LiveQuote quote2 = createLiveQuote(new BigDecimal("24505.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());
    String secondMessage = captor.getAllValues().get(1);
    assertTrue(secondMessage.contains("BEARISH REVERSAL"));
    assertTrue(secondMessage.contains("*Spot Price:* 24505"));
    assertTrue(secondMessage.contains("24580"));
    assertTrue(secondMessage.contains("75 points from 24580"));
    assertFalse(secondMessage.contains("🚨"));
    assertFalse(secondMessage.contains("🟢"));
    assertFalse(secondMessage.contains("🔴"));

    // Third tick: Further fluctuations -> NO third notification
    LiveQuote quote3 = createLiveQuote(new BigDecimal("24450.00"), baseQuoteTime.plusMinutes(2));
    strategy.execute(ticker, quote3);
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

    assertDoesNotThrow(() -> strategy.execute(ticker, quote));
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
    strategy.execute(ticker, day1Quote);
    verify(notificationService, times(1)).sendNotification(anyString());

    // Day 2 (date rollover): Quote on new day breaches new PDL at 23750 (< 23800)
    OffsetDateTime nextDayTime = baseQuoteTime.plusDays(1);
    LiveQuote day2Quote = createLiveQuote(new BigDecimal("23750.00"), nextDayTime);
    strategy.execute(ticker, day2Quote);

    // Notification 1 fires again for the new day because state was reset
    verify(notificationService, times(2)).sendNotification(anyString());
  }

  @Test
  void testMondaySession_ResolvesPreviousFridayTradingDay() {
    // Monday 2026-10-19 -> previous trading day is Friday 2026-10-16
    OffsetDateTime mondayTime =
        LocalDate.of(2026, 10, 19)
            .atTime(10, 0, 0)
            .atZone(MarketConfig.IST_ZONE)
            .toOffsetDateTime();
    LocalDate expectedFriday = LocalDate.of(2026, 10, 16);

    DailyPrice fridayPrice =
        createPreviousDailyPrice(
            expectedFriday, new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(
            eq(ticker), eq(expectedFriday), eq(PageRequest.of(0, 1))))
        .thenReturn(List.of(fridayPrice));

    LiveQuote quote = createLiveQuote(new BigDecimal("23950.00"), mondayTime);
    strategy.execute(ticker, quote);

    ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(dailyPriceRepository)
        .findLatestByTickerAndEndDate(eq(ticker), dateCaptor.capture(), eq(PageRequest.of(0, 1)));
    assertEquals(expectedFriday, dateCaptor.getValue());
    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testHolidayBeforeSession_ResolvesPriorActiveTradingDay() {
    // If Friday 2026-10-16 was a holiday, Monday 2026-10-19 resolves to Thursday 2026-10-15
    MarketTradingCalendar.setHolidays(
        Country.IN, Map.of(LocalDate.of(2026, 10, 16), "Test Holiday"));

    OffsetDateTime mondayTime =
        LocalDate.of(2026, 10, 19)
            .atTime(10, 0, 0)
            .atZone(MarketConfig.IST_ZONE)
            .toOffsetDateTime();
    LocalDate expectedThursday = LocalDate.of(2026, 10, 15);

    DailyPrice thursdayPrice =
        createPreviousDailyPrice(
            expectedThursday, new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(
            eq(ticker), eq(expectedThursday), eq(PageRequest.of(0, 1))))
        .thenReturn(List.of(thursdayPrice));

    LiveQuote quote = createLiveQuote(new BigDecimal("23950.00"), mondayTime);
    strategy.execute(ticker, quote);

    ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(dailyPriceRepository)
        .findLatestByTickerAndEndDate(eq(ticker), dateCaptor.capture(), eq(PageRequest.of(0, 1)));
    assertEquals(expectedThursday, dateCaptor.getValue());
    verify(notificationService, times(1)).sendNotification(anyString());
  }

  @Test
  void testBearReversalWithOptionRecommendation_EnrichesTelegramAlertWithPutOption() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDL at 23900
    LiveQuote quote1 = createLiveQuote(new BigDecimal("23900.00"), baseQuoteTime);
    strategy.execute(ticker, quote1);

    // Prepare mock option recommendation for PUT
    LocalDate expiry = LocalDate.of(2026, 10, 29);
    RecommendedOptionDto putOption =
        new RecommendedOptionDto(
            "NIFTY29OCT2624500PE",
            "84833",
            new BigDecimal("24500.00"),
            OptionType.PUT,
            expiry,
            13L,
            new BigDecimal("650.00"),
            new BigDecimal("600.00"),
            new BigDecimal("50.00"),
            new BigDecimal("-0.75"),
            null,
            null);

    when(niftyOptionSelectorService.selectBestOption(
            eq(ticker), eq(new BigDecimal("23970.00")), eq(OptionType.PUT), any()))
        .thenReturn(Optional.of(putOption));

    // Second tick: Rebounds by 70 points to 23970 (>= 23900 + 70)
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23970.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());
    String reversalMessage = captor.getAllValues().get(1);

    assertTrue(reversalMessage.contains("BULLISH REVERSAL"));
    assertTrue(reversalMessage.contains("RECOMMENDED OPTION"));
    assertTrue(reversalMessage.contains("BUY PUT"));
    assertTrue(reversalMessage.contains("NIFTY29OCT2624500PE"));
    assertTrue(reversalMessage.contains("24500"));
    assertTrue(reversalMessage.contains("13 DTE"));
    assertTrue(reversalMessage.contains("650"));
    assertTrue(reversalMessage.contains("600"));
    assertTrue(reversalMessage.contains("50"));
    assertTrue(reversalMessage.contains("Delta:"));
  }

  @Test
  void testBullReversalWithOptionRecommendation_EnrichesTelegramAlertWithCallOption() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDH at 24600
    LiveQuote quote1 = createLiveQuote(new BigDecimal("24600.00"), baseQuoteTime);
    strategy.execute(ticker, quote1);

    // Prepare mock option recommendation for CALL
    LocalDate expiry = LocalDate.of(2026, 10, 29);
    RecommendedOptionDto callOption =
        new RecommendedOptionDto(
            "NIFTY29OCT2624000CE",
            "84834",
            new BigDecimal("24000.00"),
            OptionType.CALL,
            expiry,
            13L,
            new BigDecimal("580.00"),
            new BigDecimal("530.00"),
            new BigDecimal("50.00"),
            null,
            null,
            null);

    when(niftyOptionSelectorService.selectBestOption(
            eq(ticker), eq(new BigDecimal("24530.00")), eq(OptionType.CALL), any()))
        .thenReturn(Optional.of(callOption));

    // Second tick: Pulls back by 70 points to 24530 (<= 24600 - 70)
    LiveQuote quote2 = createLiveQuote(new BigDecimal("24530.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());
    String reversalMessage = captor.getAllValues().get(1);

    assertTrue(reversalMessage.contains("BEARISH REVERSAL"));
    assertTrue(reversalMessage.contains("RECOMMENDED OPTION"));
    assertTrue(reversalMessage.contains("BUY CALL"));
    assertTrue(reversalMessage.contains("NIFTY29OCT2624000CE"));
    assertTrue(reversalMessage.contains("24000"));
    assertTrue(reversalMessage.contains("580"));
    assertFalse(reversalMessage.contains("Greeks:"));
  }

  @Test
  void testOptionSelectorException_CatchesGracefullyAndSendsNotificationWithFallback() {
    DailyPrice previousDayPrice =
        createPreviousDailyPrice(
            LocalDate.of(2026, 10, 15), new BigDecimal("24500.00"), new BigDecimal("24000.00"));
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(previousDayPrice));

    // First tick breaches PDL at 23900
    LiveQuote quote1 = createLiveQuote(new BigDecimal("23900.00"), baseQuoteTime);
    strategy.execute(ticker, quote1);

    when(niftyOptionSelectorService.selectBestOption(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Angel One API down"));

    // Second tick: Rebounds by 70 points
    LiveQuote quote2 = createLiveQuote(new BigDecimal("23970.00"), baseQuoteTime.plusMinutes(1));
    strategy.execute(ticker, quote2);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());
    String reversalMessage = captor.getAllValues().get(1);

    assertTrue(reversalMessage.contains("BULLISH REVERSAL"));
    assertTrue(reversalMessage.contains("No option contract found meeting the ₹500–₹900 premium"));
  }
}
