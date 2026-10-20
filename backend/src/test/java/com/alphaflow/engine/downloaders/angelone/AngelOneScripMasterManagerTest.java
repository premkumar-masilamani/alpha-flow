package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneInstrument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AngelOneScripMasterManagerTest {

  private static HttpServer mockServer;
  private static String serverBaseUrl;
  private static final AtomicInteger responseStatusCode = new AtomicInteger(200);
  private static final AtomicReference<String> responseContent = new AtomicReference<>("");

  private AngelOneConfig config;
  private ObjectMapper objectMapper;
  private AngelOneScripMasterManager manager;

  @BeforeAll
  static void startMockServer() throws Exception {
    mockServer = HttpServer.create(new InetSocketAddress(0), 0);
    mockServer.createContext(
        "/",
        exchange -> {
          int status = responseStatusCode.get();
          byte[] bytes = responseContent.get().getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
          }
        });
    mockServer.start();
    serverBaseUrl = "http://localhost:" + mockServer.getAddress().getPort();
  }

  @AfterAll
  static void stopMockServer() {
    if (mockServer != null) {
      mockServer.stop(0);
    }
  }

  @BeforeEach
  void setUp() {
    responseStatusCode.set(200);
    responseContent.set("[]");
    config = new AngelOneConfig();
    config.setScripMasterUrl(serverBaseUrl + "/scripMaster.json");
    objectMapper = new ObjectMapper();
    manager = new AngelOneScripMasterManager(config, objectMapper, RestClient.builder());
  }

  @Test
  void testGetNfoOptionsOnHolidayOrWeekendReturnsEmpty() {
    // Sunday
    LocalDate sunday = LocalDate.of(2026, 10, 18);
    List<AngelOneInstrument> instruments = manager.getNfoOptions("NIFTY", sunday);
    assertTrue(instruments.isEmpty());
  }

  @Test
  void testSuccessfulIngestionAndCaching() {
    LocalDate tradingDay = LocalDate.of(2026, 10, 16); // Friday
    String sampleJson =
        """
        [
          {
            "token": "39756",
            "symbol": "NIFTY09NOV2620300CE",
            "name": "NIFTY",
            "expiry": "09NOV2026",
            "strike": "2030000.000000",
            "lotsize": "65",
            "instrumenttype": "OPTIDX",
            "exch_seg": "NFO"
          },
          {
            "token": "39764",
            "symbol": "NIFTY09NOV2620400PE",
            "name": "NIFTY",
            "expiry": "09NOV2026",
            "strike": "20400.00",
            "lotsize": "65",
            "instrumenttype": "OPTIDX",
            "exch_seg": "NFO"
          },
          {
            "token": "99999",
            "symbol": "TCS26NOV26FUT",
            "name": "TCS",
            "expiry": "26NOV2026",
            "strike": "-1.0",
            "lotsize": "175",
            "instrumenttype": "FUTSTK",
            "exch_seg": "NFO"
          },
          {
            "token": "88888",
            "symbol": "INVALIDEXP",
            "name": "NIFTY",
            "expiry": "INVALID_DATE",
            "strike": "2050000",
            "lotsize": "65",
            "instrumenttype": "OPTIDX",
            "exch_seg": "NFO"
          },
          {
            "token": "77777",
            "symbol": "INVALIDSTRIKE",
            "name": "NIFTY",
            "expiry": "09NOV2026",
            "strike": "NOT_A_NUMBER",
            "lotsize": "65",
            "instrumenttype": "OPTIDX",
            "exch_seg": "NFO"
          }
        ]
        """;

    responseContent.set(sampleJson);

    List<AngelOneInstrument> instruments = manager.getNfoOptions("NIFTY", tradingDay);
    assertEquals(2, instruments.size());

    AngelOneInstrument first = instruments.get(0);
    assertEquals("39756", first.token());
    assertEquals("NIFTY09NOV2620300CE", first.symbol());
    assertEquals(new BigDecimal("20300.0000"), first.strike());
    assertEquals(LocalDate.of(2026, 11, 9), first.expiry());

    AngelOneInstrument second = instruments.get(1);
    assertEquals("39764", second.token());
    assertEquals("NIFTY09NOV2620400PE", second.symbol());
    assertEquals(new BigDecimal("20400.0000"), second.strike());

    // Second call on same session date returns from cache
    List<AngelOneInstrument> cached = manager.getNfoOptions("NIFTY", tradingDay);
    assertEquals(2, cached.size());

    // Underlying not found
    List<AngelOneInstrument> nonExistent = manager.getNfoOptions("BANKNIFTY", tradingDay);
    assertTrue(nonExistent.isEmpty());
  }

  @Test
  void testEmptyResponseBody() {
    LocalDate tradingDay = LocalDate.of(2026, 10, 16);
    responseContent.set("");
    List<AngelOneInstrument> instruments = manager.getNfoOptions("NIFTY", tradingDay);
    assertTrue(instruments.isEmpty());
  }

  @Test
  void testNonArrayJsonResponse() {
    LocalDate tradingDay = LocalDate.of(2026, 10, 16);
    responseContent.set("{\"status\": \"error\"}");
    List<AngelOneInstrument> instruments = manager.getNfoOptions("NIFTY", tradingDay);
    assertTrue(instruments.isEmpty());
  }

  @Test
  void testHttpServerError() {
    LocalDate tradingDay = LocalDate.of(2026, 10, 16);
    responseStatusCode.set(500);
    responseContent.set("Internal Server Error");
    List<AngelOneInstrument> instruments = manager.getNfoOptions("NIFTY", tradingDay);
    assertTrue(instruments.isEmpty());
  }

  @Test
  void testNullOrBlankExpiryAndStrike() {
    LocalDate tradingDay = LocalDate.of(2026, 10, 16);
    String sampleJson =
        """
        [
          {
            "token": "11111",
            "symbol": "NIFTYNOEXP",
            "name": "NIFTY",
            "expiry": "",
            "strike": "2050000",
            "lotsize": "65",
            "instrumenttype": "OPTIDX",
            "exch_seg": "NFO"
          },
          {
            "token": "22222",
            "symbol": "NIFTYNOSTRIKE",
            "name": "NIFTY",
            "expiry": "09NOV2026",
            "strike": "",
            "lotsize": "65",
            "instrumenttype": "OPTIDX",
            "exch_seg": "NFO"
          }
        ]
        """;
    responseContent.set(sampleJson);
    List<AngelOneInstrument> instruments = manager.getNfoOptions("NIFTY", tradingDay);
    assertTrue(instruments.isEmpty());
  }

  @Test
  void testNullRestClientBuilderFallsBackToDefault() {
    AngelOneScripMasterManager defaultManager =
        new AngelOneScripMasterManager(config, objectMapper, null);
    LocalDate sunday = LocalDate.of(2026, 10, 18);
    assertTrue(defaultManager.getNfoOptions("NIFTY", sunday).isEmpty());
  }

  @Test
  void testFutidxOnNfoAndBlankTokenFilteredOut() {
    LocalDate tradingDay = LocalDate.of(2026, 10, 16);
    String sampleJson =
        """
        [
          {
            "token": "33333",
            "symbol": "NIFTYFUT",
            "name": "NIFTY",
            "expiry": "09NOV2026",
            "strike": "-1",
            "lotsize": "65",
            "instrumenttype": "FUTIDX",
            "exch_seg": "NFO"
          },
          {
            "token": "   ",
            "symbol": "NIFTYBLANKTOKEN",
            "name": "NIFTY",
            "expiry": "09NOV2026",
            "strike": "2050000",
            "lotsize": "65",
            "instrumenttype": "OPTIDX",
            "exch_seg": "NFO"
          }
        ]
        """;
    responseContent.set(sampleJson);
    List<AngelOneInstrument> instruments = manager.getNfoOptions("NIFTY", tradingDay);
    assertTrue(instruments.isEmpty());
  }
}
