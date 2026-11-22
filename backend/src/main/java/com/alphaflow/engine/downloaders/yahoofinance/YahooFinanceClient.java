package com.alphaflow.engine.downloaders.yahoofinance;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class YahooFinanceClient {

  private static final String USER_AGENT_HEADER = "User-Agent";
  private static final String USER_AGENT_VALUE =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
  private static final String ACCEPT_HEADER = "Accept";
  private static final String ACCEPT_JSON = "application/json";
  private static final String NO_DATA_ERROR_MESSAGE = "Data doesn't exist";

  private final YahooFinanceConfig config;
  private final RestClient restClient;

  @Autowired
  public YahooFinanceClient(
      YahooFinanceConfig config,
      @Autowired(required = false) RestClient.Builder restClientBuilder) {
    this.config = config;
    this.restClient =
        (restClientBuilder != null ? restClientBuilder : RestClient.builder())
            .defaultHeader(USER_AGENT_HEADER, USER_AGENT_VALUE)
            .defaultHeader(ACCEPT_HEADER, ACCEPT_JSON)
            .build();
  }

  public YahooFinanceClient(YahooFinanceConfig config) {
    this(config, null);
  }

  public List<DailyPrice> fetchDailyPrices(Ticker ticker, long startTs, long endTs) {
    if (ticker != null) {
      String encodedSymbol = URLEncoder.encode(ticker.getTickerSymbol(), StandardCharsets.UTF_8);
      String url =
          config
              .getDownloadUrl()
              .replace("{symbol}", encodedSymbol)
              .replace("{start}", String.valueOf(startTs))
              .replace("{end}", String.valueOf(endTs));

      try {
        String json = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
        return YahooResponseParser.parse(json, ticker);
      } catch (HttpClientErrorException.BadRequest badRequestException) {
        String responseBody = badRequestException.getResponseBodyAsString();
        if (Strings.CI.contains(responseBody, NO_DATA_ERROR_MESSAGE)) {
          log.info(
              "No Yahoo Finance trading data available for {} in requested window ({} to {}); "
                  + "likely a weekend or exchange holiday.",
              ticker.getTickerSymbol(),
              startTs,
              endTs);
        } else {
          log.error(
              "Failed to download Yahoo Finance data for {}: {}",
              ticker.getTickerSymbol(),
              badRequestException.getMessage(),
              badRequestException);
        }
        return Collections.emptyList();
      } catch (RestClientException | IOException exception) {
        log.error(
            "Failed to download Yahoo Finance data for {}: {}",
            ticker.getTickerSymbol(),
            exception.getMessage(),
            exception);
        return Collections.emptyList();
      }
    } else {
      log.warn("Cannot fetch Yahoo Finance daily prices: ticker is null.");
      return Collections.emptyList();
    }
  }
}
