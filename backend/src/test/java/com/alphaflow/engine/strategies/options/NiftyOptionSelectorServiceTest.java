package com.alphaflow.engine.strategies.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.AngelOneScripMasterManager;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneInstrument;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneOptionGreek;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NiftyOptionSelectorServiceTest {

  private AngelOneScripMasterManager scripMasterManager;
  private AngelOneClient angelOneClient;
  private NiftyOptionSelectorService service;
  private Ticker niftyTicker;
  private LocalDate sessionDate;

  @BeforeEach
  void setUp() {
    scripMasterManager = mock(AngelOneScripMasterManager.class);
    angelOneClient = mock(AngelOneClient.class);
    service = new NiftyOptionSelectorService(scripMasterManager, angelOneClient);

    niftyTicker = Ticker.builder().tickerSymbol("NIFTY50").country(Country.IN).build();
    sessionDate = LocalDate.of(2026, 10, 16); // Friday (Trading Day)
  }

  @Test
  void testSelectPutOptionLowestTimeValue() {
    LocalDate targetExpiry = sessionDate.plusDays(12); // 12 DTE
    BigDecimal spotPrice = new BigDecimal("24850.00");

    AngelOneInstrument putContract1 =
        new AngelOneInstrument(
            "1001", "NIFTY28OCT2625400PE", "NIFTY", targetExpiry, new BigDecimal("25400"), 65, "OPTIDX", "NFO");
    AngelOneInstrument putContract2 =
        new AngelOneInstrument(
            "1002", "NIFTY28OCT2625500PE", "NIFTY", targetExpiry, new BigDecimal("25500"), 65, "OPTIDX", "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate))
        .thenReturn(List.of(putContract1, putContract2));

    // Contract 1: Strike 25400, Spot 24850 -> Intrinsic Value = 550. LTP = 620 -> TV = 70.
    // Contract 2: Strike 25500, Spot 24850 -> Intrinsic Value = 650. LTP = 750 -> TV = 100.
    when(angelOneClient.getMarketQuotes(eq(MarketConfig.EXCHANGE_SEGMENT_NFO), any()))
        .thenReturn(
            Map.of(
                "1001", new AngelOneQuote(new BigDecimal("620.00"), "2026-10-16T10:00:00+05:30"),
                "1002", new AngelOneQuote(new BigDecimal("750.00"), "2026-10-16T10:00:00+05:30")));

    AngelOneOptionGreek greek =
        new AngelOneOptionGreek(
            new BigDecimal("25400"),
            OptionType.PUT,
            new BigDecimal("-0.72"),
            new BigDecimal("0.001"),
            new BigDecimal("-8.5"),
            new BigDecimal("1.2"),
            new BigDecimal("14.2"),
            new BigDecimal("15000"));

    when(angelOneClient.getOptionGreeks(eq("NIFTY"), any())).thenReturn(List.of(greek));

    Optional<RecommendedOptionDto> resultOpt =
        service.selectBestOption(niftyTicker, spotPrice, OptionType.PUT, sessionDate);

    assertTrue(resultOpt.isPresent());
    RecommendedOptionDto result = resultOpt.get();
    assertEquals("NIFTY28OCT2625400PE", result.tradingSymbol());
    assertEquals("1001", result.token());
    assertEquals(0, new BigDecimal("25400").compareTo(result.strikePrice()));
    assertEquals(OptionType.PUT, result.optionType());
    assertEquals(targetExpiry, result.expiryDate());
    assertEquals(12L, result.daysToExpiry());
    assertEquals(0, new BigDecimal("620.00").compareTo(result.lastPrice()));
    assertEquals(0, new BigDecimal("550.00").compareTo(result.intrinsicValue()));
    assertEquals(0, new BigDecimal("70.00").compareTo(result.timeValue()));
    assertNotNull(result.delta());
    assertEquals(new BigDecimal("-0.72"), result.delta());
    assertEquals(new BigDecimal("14.2"), result.impliedVolatility());
  }

  @Test
  void testSelectCallOptionLowestTimeValue() {
    LocalDate targetExpiry = sessionDate.plusDays(14); // 14 DTE
    BigDecimal spotPrice = new BigDecimal("25000.00");

    // Call 1: Strike 24500, Spot 25000 -> Intrinsic = 500. LTP = 560 -> TV = 60.
    // Call 2: Strike 24400, Spot 25000 -> Intrinsic = 600. LTP = 680 -> TV = 80.
    AngelOneInstrument call1 =
        new AngelOneInstrument(
            "2001", "NIFTY30OCT2624500CE", "NIFTY", targetExpiry, new BigDecimal("24500"), 65, "OPTIDX", "NFO");
    AngelOneInstrument call2 =
        new AngelOneInstrument(
            "2002", "NIFTY30OCT2624400CE", "NIFTY", targetExpiry, new BigDecimal("24400"), 65, "OPTIDX", "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate))
        .thenReturn(List.of(call1, call2));

    when(angelOneClient.getMarketQuotes(eq(MarketConfig.EXCHANGE_SEGMENT_NFO), any()))
        .thenReturn(
            Map.of(
                "2001", new AngelOneQuote(new BigDecimal("560.00"), "2026-10-16T10:00:00+05:30"),
                "2002", new AngelOneQuote(new BigDecimal("680.00"), "2026-10-16T10:00:00+05:30")));

    when(angelOneClient.getOptionGreeks(eq("NIFTY"), any()))
        .thenReturn(Collections.emptyList());

    Optional<RecommendedOptionDto> resultOpt =
        service.selectBestOption(niftyTicker, spotPrice, OptionType.CALL, sessionDate);

    assertTrue(resultOpt.isPresent());
    RecommendedOptionDto result = resultOpt.get();
    assertEquals("NIFTY30OCT2624500CE", result.tradingSymbol());
    assertEquals(0, new BigDecimal("60.00").compareTo(result.timeValue()));
    assertNull(result.delta());
  }

  @Test
  void testDeterministicTieBreakingOnEqualTimeValue() {
    LocalDate targetExpiry = sessionDate.plusDays(10);
    BigDecimal spotPrice = new BigDecimal("25000.00");

    AngelOneInstrument callA =
        new AngelOneInstrument(
            "3001", "NIFTY26OCT2624500CE", "NIFTY", targetExpiry, new BigDecimal("24500"), 65, "OPTIDX", "NFO");
    AngelOneInstrument callB =
        new AngelOneInstrument(
            "3002", "NIFTY26OCT2624400CE", "NIFTY", targetExpiry, new BigDecimal("24400"), 65, "OPTIDX", "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate))
        .thenReturn(List.of(callA, callB));

    // Both have TV = 50.00
    // Call A: 24500 -> IV = 500, LTP = 550 -> TV = 50
    // Call B: 24400 -> IV = 600, LTP = 650 -> TV = 50
    // Call B has lower strike (24400 < 24500) -> should pick Call B
    when(angelOneClient.getMarketQuotes(eq(MarketConfig.EXCHANGE_SEGMENT_NFO), any()))
        .thenReturn(
            Map.of(
                "3001", new AngelOneQuote(new BigDecimal("550.00"), "2026-10-16T10:00:00+05:30"),
                "3002", new AngelOneQuote(new BigDecimal("650.00"), "2026-10-16T10:00:00+05:30")));

    Optional<RecommendedOptionDto> resultOpt =
        service.selectBestOption(niftyTicker, spotPrice, OptionType.CALL, sessionDate);

    assertTrue(resultOpt.isPresent());
    assertEquals("NIFTY26OCT2624400CE", resultOpt.get().tradingSymbol());
  }

  @Test
  void testSkipOnHolidayOrWeekend() {
    LocalDate sunday = LocalDate.of(2026, 10, 18);
    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, new BigDecimal("25000"), OptionType.PUT, sunday);
    assertTrue(result.isEmpty());
  }

  @Test
  void testSkipOnNonNiftyTicker() {
    Ticker reliance = Ticker.builder().tickerSymbol("RELIANCE.NS").country(Country.IN).build();
    Optional<RecommendedOptionDto> result =
        service.selectBestOption(reliance, new BigDecimal("2500"), OptionType.PUT, sessionDate);
    assertTrue(result.isEmpty());

    Optional<RecommendedOptionDto> nullTickerResult =
        service.selectBestOption(null, new BigDecimal("2500"), OptionType.PUT, sessionDate);
    assertTrue(nullTickerResult.isEmpty());
  }

  @Test
  void testNoMatchingInstrumentsInScripMaster() {
    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate)).thenReturn(Collections.emptyList());
    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, new BigDecimal("25000"), OptionType.PUT, sessionDate);
    assertTrue(result.isEmpty());
  }

  @Test
  void testNoExpiryWithinDteWindow() {
    LocalDate tooSoonExpiry = sessionDate.plusDays(5); // 5 DTE (< 10)
    LocalDate tooFarExpiry = sessionDate.plusDays(25); // 25 DTE (> 16)

    AngelOneInstrument inst1 =
        new AngelOneInstrument("1", "NIFTY01PE", "NIFTY", tooSoonExpiry, new BigDecimal("25000"), 65, "OPTIDX", "NFO");
    AngelOneInstrument inst2 =
        new AngelOneInstrument("2", "NIFTY02PE", "NIFTY", tooFarExpiry, new BigDecimal("25000"), 65, "OPTIDX", "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate)).thenReturn(List.of(inst1, inst2));

    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, new BigDecimal("25000"), OptionType.PUT, sessionDate);
    assertTrue(result.isEmpty());
  }

  @Test
  void testEmptyQuotesReturnedFromClient() {
    LocalDate targetExpiry = sessionDate.plusDays(12);
    AngelOneInstrument putInst =
        new AngelOneInstrument("1", "NIFTY28OCT2625000PE", "NIFTY", targetExpiry, new BigDecimal("25000"), 65, "OPTIDX", "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate)).thenReturn(List.of(putInst));
    when(angelOneClient.getMarketQuotes(any(), any())).thenReturn(Collections.emptyMap());

    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, new BigDecimal("25000"), OptionType.PUT, sessionDate);
    assertTrue(result.isEmpty());
  }

  @Test
  void testNoQuotesWithinPremiumRange() {
    LocalDate targetExpiry = sessionDate.plusDays(12);
    AngelOneInstrument putInst =
        new AngelOneInstrument("1", "NIFTY28OCT2625000PE", "NIFTY", targetExpiry, new BigDecimal("25000"), 65, "OPTIDX", "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate)).thenReturn(List.of(putInst));
    // Premium is 450 (below 500)
    when(angelOneClient.getMarketQuotes(any(), any()))
        .thenReturn(
            Map.of("1", new AngelOneQuote(new BigDecimal("450.00"), "2026-10-16T10:00:00+05:30")));

    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, new BigDecimal("25000"), OptionType.PUT, sessionDate);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGreekEnrichmentExceptionDoesNotCrash() {
    LocalDate targetExpiry = sessionDate.plusDays(12);
    AngelOneInstrument callInst =
        new AngelOneInstrument("1", "NIFTY28OCT2624500CE", "NIFTY", targetExpiry, new BigDecimal("24500"), 65, "OPTIDX", "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate)).thenReturn(List.of(callInst));
    when(angelOneClient.getMarketQuotes(any(), any()))
        .thenReturn(
            Map.of("1", new AngelOneQuote(new BigDecimal("600.00"), "2026-10-16T10:00:00+05:30")));
    when(angelOneClient.getOptionGreeks(any(), any())).thenThrow(new RuntimeException("API error"));

    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, new BigDecimal("25000"), OptionType.CALL, sessionDate);

    assertTrue(result.isPresent());
    assertNull(result.get().delta());
  }

  @Test
  void testCandidateBatchSizeCap() {
    LocalDate targetExpiry = sessionDate.plusDays(12);

    List<AngelOneInstrument> instruments = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      BigDecimal strike = new BigDecimal(24500 + i * 5);
      instruments.add(
          new AngelOneInstrument(
              String.valueOf(100 + i),
              "NIFTY" + strike + "CE",
              "NIFTY",
              targetExpiry,
              strike,
              65,
              "OPTIDX",
              "NFO"));
    }

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate)).thenReturn(instruments);
    when(angelOneClient.getMarketQuotes(any(), any()))
        .thenReturn(
            Map.of("159", new AngelOneQuote(new BigDecimal("600"), "2026-10-16T10:00:00+05:30")));

    BigDecimal spotPrice = new BigDecimal("25000");

    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, spotPrice, OptionType.CALL, sessionDate);
    assertTrue(result.isPresent());
  }

  @Test
  void testSelectOtmOptionWithZeroIntrinsicValue() {
    LocalDate targetExpiry = sessionDate.plusDays(12);
    BigDecimal spotPrice = new BigDecimal("25000");

    // Strike 25100 > Spot 25000 for CE -> Intrinsic Value is 0
    AngelOneInstrument otmCall =
        new AngelOneInstrument(
            "9001",
            "NIFTY28OCT2625100CE",
            "NIFTY",
            targetExpiry,
            new BigDecimal("25100"),
            65,
            "OPTIDX",
            "NFO");

    when(scripMasterManager.getNfoOptions("NIFTY", sessionDate)).thenReturn(List.of(otmCall));
    when(angelOneClient.getMarketQuotes(any(), any()))
        .thenReturn(
            Map.of(
                "9001",
                new AngelOneQuote(new BigDecimal("550.00"), "2026-10-16T10:00:00+05:30")));

    Optional<RecommendedOptionDto> result =
        service.selectBestOption(niftyTicker, spotPrice, OptionType.CALL, sessionDate);

    assertTrue(result.isPresent());
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get().intrinsicValue()));
    assertEquals(0, new BigDecimal("550.00").compareTo(result.get().timeValue()));
  }

  @Test
  void testNullTickerSymbol() {
    Ticker nullSymbolTicker = Ticker.builder().tickerSymbol(null).country(Country.IN).build();
    Optional<RecommendedOptionDto> result =
        service.selectBestOption(nullSymbolTicker, new BigDecimal("25000"), OptionType.CALL, sessionDate);
    assertTrue(result.isEmpty());
  }
}
