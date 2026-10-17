package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneInstrument;
import com.alphaflow.persistence.enums.Country;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AngelOneScripMasterManager {

  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final BigDecimal STRIKE_SCALE_THRESHOLD = new BigDecimal("100000");
  private static final DateTimeFormatter EXPIRY_FORMATTER =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("ddMMMyyyy")
          .toFormatter(Locale.ENGLISH);

  private final AngelOneConfig config;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final ConcurrentMap<String, List<AngelOneInstrument>> cache = new ConcurrentHashMap<>();
  private volatile LocalDate cachedSessionDate;

  @Autowired
  public AngelOneScripMasterManager(
      AngelOneConfig config,
      ObjectMapper objectMapper,
      @Autowired(required = false) RestClient.Builder restClientBuilder) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.restClient =
        (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build();
  }

  public List<AngelOneInstrument> getNfoOptions(String underlyingName, LocalDate sessionDate) {
    if (MarketTradingCalendar.isTradingDay(sessionDate, Country.IN)) {
      if (!sessionDate.equals(cachedSessionDate)) {
        refreshCache(sessionDate);
      }
      List<AngelOneInstrument> instruments = cache.get(underlyingName.toUpperCase(Locale.ROOT));
      if (instruments != null) {
        return instruments;
      } else {
        return Collections.emptyList();
      }
    } else {
      log.debug("Skipping NFO options sync: {} is not an active Indian trading day.", sessionDate);
      return Collections.emptyList();
    }
  }

  private synchronized void refreshCache(LocalDate sessionDate) {
    if (!sessionDate.equals(cachedSessionDate)) {
      cache.clear();
      log.info("Refreshing Angel One NFO Scrip Master for session date {}", sessionDate);

      try {
        String responseBody =
            restClient
                .get()
                .uri(config.getScripMasterUrl())
                .retrieve()
                .body(String.class);

        if (responseBody != null && !responseBody.isBlank()) {
          JsonNode rootNode = objectMapper.readTree(responseBody);
          if (rootNode.isArray()) {
            for (JsonNode rowNode : rootNode) {
              String exchangeSegment = rowNode.path("exch_seg").asText();
              String instrumentType = rowNode.path("instrumenttype").asText();

              if (MarketConstants.EXCHANGE_SEGMENT_NFO.equalsIgnoreCase(exchangeSegment)
                  && "OPTIDX".equalsIgnoreCase(instrumentType)) {
                String name = rowNode.path("name").asText().toUpperCase(Locale.ROOT);
                String token = rowNode.path("token").asText();
                String symbol = rowNode.path("symbol").asText();
                String rawExpiry = rowNode.path("expiry").asText();
                String rawStrike = rowNode.path("strike").asText();
                int lotSize = rowNode.path("lotsize").asInt(1);

                LocalDate expiry = parseExpiryDate(rawExpiry);
                BigDecimal strike = normalizeStrike(rawStrike);

                if (expiry != null && strike != null && !token.isBlank()) {
                  AngelOneInstrument instrument =
                      new AngelOneInstrument(
                          token,
                          symbol,
                          name,
                          expiry,
                          strike,
                          lotSize,
                          instrumentType,
                          exchangeSegment);
                  cache.computeIfAbsent(name, key -> new ArrayList<>()).add(instrument);
                }
              }
            }
            cachedSessionDate = sessionDate;
            log.info(
                "Successfully refreshed Angel One NFO Scrip Master. Cached {} underlyings.",
                cache.size());
          } else {
            log.warn("Scrip master response is not a valid JSON array.");
          }
        } else {
          log.warn("Scrip master response body was empty.");
        }
      } catch (RestClientException | JsonProcessingException exception) {
        log.error(
            "Failed to fetch Angel One Scrip Master: {}", exception.getMessage(), exception);
      }
    }
  }

  private LocalDate parseExpiryDate(String rawExpiry) {
    if (rawExpiry != null && !rawExpiry.isBlank()) {
      try {
        return LocalDate.parse(rawExpiry.trim(), EXPIRY_FORMATTER);
      } catch (Exception exception) {
        log.debug("Unable to parse expiry date '{}': {}", rawExpiry, exception.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  private BigDecimal normalizeStrike(String rawStrike) {
    if (rawStrike != null && !rawStrike.isBlank()) {
      try {
        BigDecimal parsedStrike = new BigDecimal(rawStrike.trim());
        if (parsedStrike.compareTo(STRIKE_SCALE_THRESHOLD) >= 0) {
          return parsedStrike.divide(HUNDRED, 4, RoundingMode.HALF_UP);
        } else {
          return parsedStrike.setScale(4, RoundingMode.HALF_UP);
        }
      } catch (Exception exception) {
        log.debug("Unable to parse strike price '{}': {}", rawStrike, exception.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }
}
