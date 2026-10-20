package com.alphaflow.engine.strategies;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.engine.services.NotificationTemplateEngine;
import com.alphaflow.engine.strategies.options.NiftyOptionSelectorService;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * End-to-end test case that simulates the full 70-point reversal flow
 * with option selection and prints the exact generated notification payload.
 */
class Intraday70PBounceNotificationFlowTest {

  private DailyPriceRepository dailyPriceRepository;
  private NotificationService notificationService;
  private NiftyOptionSelectorService niftyOptionSelectorService;
  private Intraday70PBounceStrategy strategy;
  private Ticker niftyTicker;
  private OffsetDateTime quoteTime;

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

    niftyTicker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50.NS")
            .build();
    quoteTime = OffsetDateTime.now(MarketConfig.IST_ZONE);
  }

  @Test
  void testCompleteBearReversalFlow_PrintsAndVerifiesNotification() {
    // 1. Previous Day: High = 25000, Low = 24800 (PDL = 24800)
    DailyPrice yesterday =
        DailyPrice.builder()
            .ticker(niftyTicker)
            .priceDate(quoteTime.toLocalDate().minusDays(1))
            .priceHigh(new BigDecimal("25000.00"))
            .priceLow(new BigDecimal("24800.00"))
            .build();
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(yesterday));

    // 2. Tick 1: Spot breaches PDL -> Drops to 24700 (Day's Low = 24700)
    LiveQuote breachQuote =
        LiveQuote.builder()
            .ticker(niftyTicker)
            .lastPrice(new BigDecimal("24700.00"))
            .quoteTime(quoteTime)
            .build();
    strategy.execute(niftyTicker, breachQuote);

    // 3. Setup mock selected PUT Option (DTE 12, Premium 650, TV 220)
    LocalDate targetExpiry = quoteTime.toLocalDate().plusDays(12);
    RecommendedOptionDto bestPutOption =
        new RecommendedOptionDto(
            "NIFTY29OCT2625200PE",
            "84833",
            new BigDecimal("25200.00"),
            OptionType.PUT,
            targetExpiry,
            12L,
            new BigDecimal("650.00"),
            new BigDecimal("430.00"),
            new BigDecimal("220.00"));

    when(niftyOptionSelectorService.selectBestOption(
            eq(niftyTicker), eq(new BigDecimal("24770.00")), eq(OptionType.PUT), any()))
        .thenReturn(Optional.of(bestPutOption));

    // 4. Tick 2: Rebounds by 70 points to 24770 (>= 24700 + 70)
    LiveQuote reboundQuote =
        LiveQuote.builder()
            .ticker(niftyTicker)
            .lastPrice(new BigDecimal("24770.00"))
            .quoteTime(quoteTime.plusMinutes(2))
            .build();
    strategy.execute(niftyTicker, reboundQuote);

    // 5. Verify notifications sent (1 breach alert + 1 reversal alert)
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());

    String reversalAlert = captor.getAllValues().get(1);

    // Print to stdout for developer visibility
    System.out.println("========== FULL BEAR REVERSAL NOTIFICATION ==========");
    System.out.println(reversalAlert);
    System.out.println("=====================================================");

    assertTrue(reversalAlert.contains("BULLISH REVERSAL"));
    assertTrue(reversalAlert.contains("BUY PUT"));
    assertTrue(reversalAlert.contains("NIFTY29OCT2625200PE"));
    assertTrue(reversalAlert.contains("25200"));
    assertTrue(reversalAlert.contains("12 DTE"));
    assertTrue(reversalAlert.contains("Option Premium"));
    assertTrue(reversalAlert.contains("650"));
    assertTrue(reversalAlert.contains("Intrinsic Value"));
    assertTrue(reversalAlert.contains("430"));
    assertTrue(reversalAlert.contains("Time Value"));
    assertTrue(reversalAlert.contains("220"));
  }

  @Test
  void testCompleteBullReversalFlow_PrintsAndVerifiesNotification() {
    // 1. Previous Day: High = 25200, Low = 25000 (PDH = 25200)
    DailyPrice yesterday =
        DailyPrice.builder()
            .ticker(niftyTicker)
            .priceDate(quoteTime.toLocalDate().minusDays(1))
            .priceHigh(new BigDecimal("25200.00"))
            .priceLow(new BigDecimal("25000.00"))
            .build();
    when(dailyPriceRepository.findLatestByTickerAndEndDate(any(), any(), any()))
        .thenReturn(List.of(yesterday));

    // 2. Tick 1: Spot breaches PDH -> Rallies to 25300 (Day's High = 25300)
    LiveQuote breachQuote =
        LiveQuote.builder()
            .ticker(niftyTicker)
            .lastPrice(new BigDecimal("25300.00"))
            .quoteTime(quoteTime)
            .build();
    strategy.execute(niftyTicker, breachQuote);

    // 3. Setup mock selected CALL Option (DTE 12, Premium 580, TV 150)
    LocalDate targetExpiry = quoteTime.toLocalDate().plusDays(12);
    RecommendedOptionDto bestCallOption =
        new RecommendedOptionDto(
            "NIFTY29OCT2624800CE",
            "84834",
            new BigDecimal("24800.00"),
            OptionType.CALL,
            targetExpiry,
            12L,
            new BigDecimal("580.00"),
            new BigDecimal("430.00"),
            new BigDecimal("150.00"));

    when(niftyOptionSelectorService.selectBestOption(
            eq(niftyTicker), eq(new BigDecimal("25230.00")), eq(OptionType.CALL), any()))
        .thenReturn(Optional.of(bestCallOption));

    // 4. Tick 2: Pulls back by 70 points to 25230 (<= 25300 - 70)
    LiveQuote pullbackQuote =
        LiveQuote.builder()
            .ticker(niftyTicker)
            .lastPrice(new BigDecimal("25230.00"))
            .quoteTime(quoteTime.plusMinutes(2))
            .build();
    strategy.execute(niftyTicker, pullbackQuote);

    // 5. Verify notifications sent
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).sendNotification(captor.capture());

    String reversalAlert = captor.getAllValues().get(1);

    // Print to stdout for developer visibility
    System.out.println("========== FULL BULL REVERSAL NOTIFICATION ==========");
    System.out.println(reversalAlert);
    System.out.println("=====================================================");

    assertTrue(reversalAlert.contains("BEARISH REVERSAL"));
    assertTrue(reversalAlert.contains("BUY CALL"));
    assertTrue(reversalAlert.contains("NIFTY29OCT2624800CE"));
    assertTrue(reversalAlert.contains("24800"));
    assertTrue(reversalAlert.contains("Option Premium"));
    assertTrue(reversalAlert.contains("580"));
  }
}
