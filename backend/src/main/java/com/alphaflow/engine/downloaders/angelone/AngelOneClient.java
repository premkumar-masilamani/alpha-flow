package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneOptionGreek;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AngelOneClient {

  private static final ZoneId IST_ZONE = MarketConfig.IST_ZONE;
  private static final DateTimeFormatter CANDLE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  private final AngelOneConfig config;
  private final AngelOneAuthManager authManager;
  private final ObjectMapper objectMapper;
  private final AngelOneNetworkHelper networkHelper;
  private final AngelOneRateLimiter rateLimiter;
  private final RestClient restClient;

  public AngelOneClient(
      AngelOneConfig config,
      AngelOneAuthManager authManager,
      ObjectMapper objectMapper,
      AngelOneNetworkHelper networkHelper,
      AngelOneRateLimiter rateLimiter,
      @org.springframework.beans.factory.annotation.Autowired(required = false)
          RestClient.Builder restClientBuilder) {
    this.config = config;
    this.authManager = authManager;
    this.objectMapper = objectMapper;
    this.networkHelper = networkHelper;
    this.rateLimiter = rateLimiter;
    this.restClient =
        (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build();
  }

  public static String resolveToken(String symbol) {
    if ("NIFTY50".equalsIgnoreCase(symbol) || "NIFTY 50".equalsIgnoreCase(symbol)) {
      return "99926000";
    }
    return symbol;
  }

  public static String mapToAngelOneInterval(IntradayTimeframe timeframe) {
    if (timeframe == IntradayTimeframe._15M) {
      return "FIFTEEN_MINUTE";
    }
    return timeframe != null ? timeframe.getValue() : "";
  }

  public static final int ANGEL_ONE_15M_MAX_CHUNK_DAYS = 200;
  private static final DateTimeFormatter CHUNK_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public List<AngelOneCandle> fetch15mCandles(
      String exchange,
      String symbolToken,
      OffsetDateTime startWindow,
      OffsetDateTime expectedCompletedTime) {
    if (exchange != null && symbolToken != null && startWindow != null && expectedCompletedTime != null) {
      Map<OffsetDateTime, AngelOneCandle> candleMap = new LinkedHashMap<>();
      OffsetDateTime currentWindowStart = startWindow;
      while (currentWindowStart.isBefore(expectedCompletedTime)) {
        OffsetDateTime candidateWindowEnd =
            currentWindowStart.plusDays(ANGEL_ONE_15M_MAX_CHUNK_DAYS);
        OffsetDateTime currentWindowEnd =
            candidateWindowEnd.isBefore(expectedCompletedTime)
                ? candidateWindowEnd
                : expectedCompletedTime;

        String fromDateStr = currentWindowStart.format(CHUNK_DATE_TIME_FORMATTER);
        String toDateStr = currentWindowEnd.format(CHUNK_DATE_TIME_FORMATTER);

        log.info(
            "Syncing 15m candles for token {} from {} to {}",
            symbolToken,
            fromDateStr,
            toDateStr);

        List<AngelOneCandle> chunk =
            getCandleData(exchange, symbolToken, IntradayTimeframe._15M, fromDateStr, toDateStr);
        if (chunk != null) {
          for (AngelOneCandle candle : chunk) {
            candleMap.putIfAbsent(candle.timestamp(), candle);
          }
        }

        currentWindowStart = currentWindowEnd.plusMinutes(1);
      }
      return new ArrayList<>(candleMap.values());
    } else {
      log.warn("Cannot fetch 15m candles: missing required parameters.");
      return Collections.emptyList();
    }
  }

  public List<AngelOneCandle> getCandleData(
      String exchange,
      String symbolToken,
      IntradayTimeframe timeframe,
      String fromDate,
      String toDate) {

    String angelOneInterval = mapToAngelOneInterval(timeframe);

    Map<String, String> body = new HashMap<>();
    body.put("exchange", exchange);
    body.put("symboltoken", symbolToken);
    body.put("interval", angelOneInterval);
    body.put("fromdate", fromDate);
    body.put("todate", toDate);

    String jwt = authManager.getValidJwtToken();
    if (jwt == null || jwt.isBlank()) {
      log.warn("Cannot fetch candle data: no valid Angel One JWT token.");
      return Collections.emptyList();
    }

    try {
      rateLimiter.throttle();
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
      if (responseBody != null) {
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
      } else {
        log.warn("Empty response body from getCandleData");
      }
    } catch (RestClientException | JsonProcessingException e) {
      log.warn("Failed to fetch candle data from Angel One: {}", e.getMessage());
    }
    return Collections.emptyList();
  }

  public Optional<AngelOneQuote> getMarketQuote(String exchange, String symbolToken) {
    Map<String, Object> body = new HashMap<>();
    body.put("mode", "FULL");
    body.put("exchangeTokens", Map.of(exchange, List.of(symbolToken)));

    String jwt = authManager.getValidJwtToken();
    if (jwt == null || jwt.isBlank()) {
      return Optional.empty();
    }

    try {
      rateLimiter.throttle();
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
      if (responseBody != null) {
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
      }
    } catch (RestClientException | JsonProcessingException e) {
      log.warn("Failed to fetch quote from Angel One: {}", e.getMessage());
    }
    return Optional.empty();
  }

  public Map<String, BigDecimal> getMarketQuotes(String exchange, List<String> symbolTokens) {
    if (symbolTokens != null && !symbolTokens.isEmpty()) {
      Map<String, Object> body = new HashMap<>();
      body.put("mode", "FULL");
      body.put("exchangeTokens", Map.of(exchange, symbolTokens));

      String jwt = authManager.getValidJwtToken();
      if (jwt == null || jwt.isBlank()) {
        log.warn("Cannot fetch market quotes: no valid Angel One JWT token.");
        return Collections.emptyMap();
      }

      try {
        rateLimiter.throttle();
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
        if (responseBody != null) {
          JsonNode root = objectMapper.readTree(responseBody);
          if (root.path("status").asBoolean(false)) {
            JsonNode fetchedList = root.path("data").path("fetched");
            if (fetchedList.isArray()) {
              Map<String, BigDecimal> quotes = new HashMap<>();
              for (JsonNode item : fetchedList) {
                String token = item.path("symbolToken").asText();
                if (!token.isBlank()) {
                  BigDecimal ltp =
                      item.hasNonNull("ltp")
                          ? new BigDecimal(item.path("ltp").asText())
                          : BigDecimal.ZERO;
                  quotes.put(token, ltp);
                }
              }
              return quotes;
            }
          } else {
            String errorCode = root.path("errorcode").asText();
            if ("AG8001".equalsIgnoreCase(errorCode)) {
              authManager.invalidateSession();
            }
          }
        }
      } catch (RestClientException | JsonProcessingException e) {
        log.warn("Failed to fetch quotes from Angel One: {}", e.getMessage());
      }
      return Collections.emptyMap();
    } else {
      return Collections.emptyMap();
    }
  }

  public List<AngelOneOptionGreek> getOptionGreeks(String underlyingName, String expiryDate) {
    if (underlyingName != null
        && !underlyingName.isBlank()
        && expiryDate != null
        && !expiryDate.isBlank()) {
      Map<String, String> body = new HashMap<>();
      body.put("name", underlyingName);
      body.put("expirydate", expiryDate);

      String jwt = authManager.getValidJwtToken();
      if (jwt == null || jwt.isBlank()) {
        log.warn("Cannot fetch option greeks: no valid Angel One JWT token.");
        return Collections.emptyList();
      }

      try {
        rateLimiter.throttle();
        ResponseEntity<String> response =
            restClient
                .post()
                .uri(config.getOptionGreeksUrl())
                .headers(
                    headers ->
                        networkHelper.applyAuthenticatedHeaders(headers, config.getApiKey(), jwt))
                .body(body)
                .retrieve()
                .toEntity(String.class);

        String responseBody = response.getBody();
        if (responseBody != null) {
          JsonNode root = objectMapper.readTree(responseBody);
          if (root.path("status").asBoolean(false)) {
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
              List<AngelOneOptionGreek> greeks = new ArrayList<>();
              for (JsonNode row : dataNode) {
                BigDecimal strike =
                    row.hasNonNull("strikePrice")
                        ? new BigDecimal(row.path("strikePrice").asText())
                        : null;
                String optionTypeStr = row.path("optionType").asText();
                OptionType optionType =
                    !optionTypeStr.isBlank() ? OptionType.from(optionTypeStr) : null;
                BigDecimal delta =
                    row.hasNonNull("delta") ? new BigDecimal(row.path("delta").asText()) : null;
                BigDecimal gamma =
                    row.hasNonNull("gamma") ? new BigDecimal(row.path("gamma").asText()) : null;
                BigDecimal theta =
                    row.hasNonNull("theta") ? new BigDecimal(row.path("theta").asText()) : null;
                BigDecimal vega =
                    row.hasNonNull("vega") ? new BigDecimal(row.path("vega").asText()) : null;
                BigDecimal iv =
                    row.hasNonNull("impliedVolatility")
                        ? new BigDecimal(row.path("impliedVolatility").asText())
                        : null;
                BigDecimal volume =
                    row.hasNonNull("tradeVolume")
                        ? new BigDecimal(row.path("tradeVolume").asText())
                        : null;

                if (strike != null && optionType != null) {
                  greeks.add(
                      new AngelOneOptionGreek(
                          strike, optionType, delta, gamma, theta, vega, iv, volume));
                }
              }
              return greeks;
            }
          } else {
            String errorCode = root.path("errorcode").asText();
            if ("AG8001".equalsIgnoreCase(errorCode)) {
              authManager.invalidateSession();
            }
          }
        }
      } catch (RestClientException | JsonProcessingException e) {
        log.warn("Failed to fetch option greeks from Angel One: {}", e.getMessage());
      }
      return Collections.emptyList();
    } else {
      return Collections.emptyList();
    }
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
}
