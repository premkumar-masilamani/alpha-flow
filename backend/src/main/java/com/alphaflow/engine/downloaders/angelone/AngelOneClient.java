package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class AngelOneClient {

  private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
  private static final DateTimeFormatter CANDLE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  private final AngelOneConfig config;
  private final AngelOneAuthManager authManager;
  private final ObjectMapper objectMapper;
  private final AngelOneNetworkHelper networkHelper;
  private final RestClient restClient;

  public AngelOneClient(
      AngelOneConfig config,
      AngelOneAuthManager authManager,
      ObjectMapper objectMapper,
      AngelOneNetworkHelper networkHelper,
      RestClient.Builder restClientBuilder) {
    this.config = config;
    this.authManager = authManager;
    this.objectMapper = objectMapper;
    this.networkHelper = networkHelper;
    this.restClient = restClientBuilder.build();
  }

  public static String resolveToken(String symbol) {
    if ("NIFTY50".equalsIgnoreCase(symbol) || "NIFTY 50".equalsIgnoreCase(symbol)) {
      return "99926000";
    }
    return symbol;
  }

  public static String mapToAngelOneInterval(String timeframe) {
    if ("15M".equalsIgnoreCase(timeframe) || "FIFTEEN_MINUTE".equalsIgnoreCase(timeframe)) {
      return "FIFTEEN_MINUTE";
    }
    return timeframe;
  }

  public List<AngelOneCandle> getCandleData(
      String exchange, String symbolToken, String interval, String fromDate, String toDate) {

    String angelOneInterval = mapToAngelOneInterval(interval);

    Map<String, String> body = new HashMap<>();
    body.put("exchange", exchange);
    body.put("symboltoken", symbolToken);
    body.put("interval", angelOneInterval);
    body.put("fromdate", fromDate);
    body.put("todate", toDate);

    for (int attempt = 1; attempt <= 2; attempt++) {
      String jwt = authManager.getValidJwtToken();
      if (jwt == null || jwt.isBlank()) {
        log.warn("Cannot fetch candle data: no valid Angel One JWT token.");
        return Collections.emptyList();
      }

      try {
        ResponseEntity<String> response =
            restClient
                .post()
                .uri(config.getCandleUrl())
                .headers(
                    headers ->
                        networkHelper.applyAuthenticatedHeaders(headers, config.getApiKey(), jwt))
                .body(body)
                .retrieve()
                .toEntity(String.class);

        String responseBody = response.getBody();
        if (responseBody == null) {
          log.warn("Empty response body from getCandleData");
          return Collections.emptyList();
        }

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
    Map<String, Object> body = new HashMap<>();
    body.put("mode", "FULL");
    body.put("exchangeTokens", Map.of(exchange, List.of(symbolToken)));

    for (int attempt = 1; attempt <= 2; attempt++) {
      String jwt = authManager.getValidJwtToken();
      if (jwt == null || jwt.isBlank()) {
        return Optional.empty();
      }

      try {
        ResponseEntity<String> response =
            restClient
                .post()
                .uri(config.getQuoteUrl())
                .headers(
                    headers ->
                        networkHelper.applyAuthenticatedHeaders(headers, config.getApiKey(), jwt))
                .body(body)
                .retrieve()
                .toEntity(String.class);

        String responseBody = response.getBody();
        if (responseBody == null) {
          return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(responseBody);
        if (root.path("status").asBoolean(false)) {
          JsonNode fetchedList = root.path("data").path("fetched");
          if (fetchedList.isArray() && !fetchedList.isEmpty()) {
            JsonNode item = fetchedList.get(0);
            BigDecimal ltp =
                item.hasNonNull("ltp")
                    ? new BigDecimal(item.path("ltp").asText())
                    : BigDecimal.ZERO;

            String timestamp =
                item.hasNonNull("exchFeedTime")
                    ? item.path("exchFeedTime").asText()
                    : OffsetDateTime.now(IST_ZONE).toString();

            return Optional.of(new AngelOneQuote(ltp, timestamp));
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

  OffsetDateTime parseTimestamp(String raw) {
    try {
      return OffsetDateTime.parse(raw, CANDLE_TIMESTAMP_FORMATTER);
    } catch (Exception e) {
      try {
        DateTimeFormatter noOffset = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse(raw, noOffset);
        return ldt.atZone(IST_ZONE).toOffsetDateTime();
      } catch (Exception ex) {
        return OffsetDateTime.now(IST_ZONE);
      }
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
