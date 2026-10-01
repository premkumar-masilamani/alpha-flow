package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneClient {

  private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
  private final AngelOneConfig config;
  private final AngelOneAuthManager authManager;
  private final ObjectMapper objectMapper;
  private final AngelOneNetworkHelper networkHelper;

  public AngelOneClient(
      AngelOneConfig config,
      AngelOneAuthManager authManager,
      ObjectMapper objectMapper,
      AngelOneNetworkHelper networkHelper) {
    this.config = config;
    this.authManager = authManager;
    this.objectMapper = objectMapper;
    this.networkHelper = networkHelper;
  }

  public static String resolveToken(String symbol) {
    if (symbol != null && symbol.equalsIgnoreCase("NIFTY50")) {
      return "99926000";
    }
    return "99926000";
  }

  public List<AngelOneCandle> getCandleData(
      String exchange, String symbolToken, String interval, String fromDate, String toDate) {

    Map<String, String> body = new HashMap<>();
    body.put("exchange", exchange);
    body.put("symboltoken", symbolToken);
    body.put("interval", interval);
    body.put("fromdate", fromDate);
    body.put("todate", toDate);

    final String url =
        config.getBaseUrl() + "/rest/secure/angelbroking/historical/v1/getCandleData";
    for (int attempt = 1; attempt <= 2; attempt++) {
      String jwt = authManager.getValidJwtToken();
      if (jwt == null || jwt.isBlank()) {
        log.warn("Cannot fetch candle data: no valid Angel One JWT token.");
        return Collections.emptyList();
      }

      try {
        String responseBody = executePost(url, body, jwt);
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.path("status").asBoolean(false)) {
          JsonNode dataNode = root.path("data");
          if (dataNode.isArray()) {
            List<AngelOneCandle> candles = new ArrayList<>();
            for (JsonNode row : dataNode) {
              if (row.isArray() && row.size() >= 6) {
                OffsetDateTime ts = parseTimestamp(row.get(0).asText());
                BigDecimal open = new BigDecimal(row.get(1).asText());
                BigDecimal high = new BigDecimal(row.get(2).asText());
                BigDecimal low = new BigDecimal(row.get(3).asText());
                BigDecimal close = new BigDecimal(row.get(4).asText());
                BigDecimal volume = new BigDecimal(row.get(5).asText());
                candles.add(new AngelOneCandle(ts, open, high, low, close, volume));
              }
            }
            return candles;
          }
        } else {
          String message = root.path("message").asText();
          String errorCode = root.path("errorcode").asText();
          log.warn(
              "Angel One getCandleData returned status false: {} (code: {})", message, errorCode);
          if ("AG8001".equalsIgnoreCase(errorCode) || message.toLowerCase().contains("token")) {
            authManager.invalidateSession();
          }
        }
      } catch (Exception e) {
        log.warn(
            "Attempt {}/2 failed to fetch candle data from Angel One: {}", attempt, e.getMessage());
        if (attempt == 1) {
          authManager.invalidateSession();
          sleep(config.getDelayMilliseconds());
        }
      }
    }
    return Collections.emptyList();
  }

  public Optional<AngelOneQuote> getMarketQuote(String exchange, String symbolToken) {
    String url = config.getBaseUrl() + "/rest/secure/angelbroking/market/v1/quote";
    Map<String, Object> body = new HashMap<>();
    body.put("mode", "FULL");
    body.put("exchangeTokens", Map.of(exchange, List.of(symbolToken)));

    for (int attempt = 1; attempt <= 2; attempt++) {
      String jwt = authManager.getValidJwtToken();
      if (jwt == null || jwt.isBlank()) {
        return Optional.empty();
      }

      try {
        String responseBody = executePost(url, body, jwt);
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.path("status").asBoolean(false)) {
          JsonNode fetchedList = root.path("data").path("fetched");
          if (fetchedList.isArray() && !fetchedList.isEmpty()) {
            JsonNode item = fetchedList.get(0);
            BigDecimal ltp =
                item.hasNonNull("ltp")
                    ? new BigDecimal(item.path("ltp").asText())
                    : BigDecimal.ZERO;
            BigDecimal open =
                item.hasNonNull("open")
                    ? new BigDecimal(item.path("open").asText())
                    : BigDecimal.ZERO;
            BigDecimal high =
                item.hasNonNull("high")
                    ? new BigDecimal(item.path("high").asText())
                    : BigDecimal.ZERO;
            BigDecimal low =
                item.hasNonNull("low")
                    ? new BigDecimal(item.path("low").asText())
                    : BigDecimal.ZERO;
            BigDecimal close =
                item.hasNonNull("close")
                    ? new BigDecimal(item.path("close").asText())
                    : BigDecimal.ZERO;
            BigDecimal volume =
                item.hasNonNull("tradeVolume")
                    ? new BigDecimal(item.path("tradeVolume").asText())
                    : BigDecimal.ZERO;

            BigDecimal change = ltp.subtract(close);
            BigDecimal changePercent = BigDecimal.ZERO;
            if (close.compareTo(BigDecimal.ZERO) > 0) {
              changePercent =
                  change.divide(close, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
            String timestamp =
                item.hasNonNull("exchFeedTime")
                    ? item.path("exchFeedTime").asText()
                    : OffsetDateTime.now(IST_ZONE).toString();

            return Optional.of(
                new AngelOneQuote(
                    ltp, change, changePercent, open, high, low, close, volume, timestamp));
          }
        } else {
          String errorCode = root.path("errorcode").asText();
          if ("AG8001".equalsIgnoreCase(errorCode)) {
            authManager.invalidateSession();
          }
        }
      } catch (Exception e) {
        log.warn("Attempt {}/2 failed to fetch quote from Angel One: {}", attempt, e.getMessage());
        if (attempt == 1) {
          authManager.invalidateSession();
          sleep(config.getDelayMilliseconds());
        }
      }
    }
    return Optional.empty();
  }

  private String executePost(String url, Object payload, String jwtToken) throws Exception {
    byte[] requestBytes = objectMapper.writeValueAsBytes(payload);
    HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
    conn.setRequestMethod("POST");
    conn.setConnectTimeout(10000);
    conn.setReadTimeout(10000);
    conn.setDoOutput(true);
    networkHelper.applyAuthenticatedHeaders(conn, config.getApiKey(), jwtToken);

    try (OutputStream os = conn.getOutputStream()) {
      os.write(requestBytes);
    }

    int statusCode = conn.getResponseCode();
    InputStream is =
        (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();

    byte[] bytes = (is != null) ? is.readAllBytes() : new byte[0];
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private OffsetDateTime parseTimestamp(String text) {
    try {
      return OffsetDateTime.parse(text);
    } catch (Exception ignored) {
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      LocalDateTime ldt = LocalDateTime.parse(text.replace('T', ' ').substring(0, 19), dtf);
      return ldt.atZone(IST_ZONE).toOffsetDateTime();
    }
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
