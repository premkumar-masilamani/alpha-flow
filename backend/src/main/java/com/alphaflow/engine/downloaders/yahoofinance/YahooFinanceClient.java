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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

  private final RestClient restClient;

  @Autowired
  public YahooFinanceClient(@Autowired(required = false) RestClient.Builder restClientBuilder) {
    this.restClient =
        (restClientBuilder != null ? restClientBuilder : RestClient.builder())
            .defaultHeader(USER_AGENT_HEADER, USER_AGENT_VALUE)
            .defaultHeader(ACCEPT_HEADER, ACCEPT_JSON)
            .build();
  }

  public List<DailyPrice> fetchDailyPrices(Ticker ticker, long startTs, long endTs) {
    if (ticker != null) {
      String encodedSymbol = URLEncoder.encode(ticker.getTickerSymbol(), StandardCharsets.UTF_8);
      String url =
          YahooFinanceConfig.DOWNLOAD_URL
              .replace("{symbol}", encodedSymbol)
              .replace("{start}", String.valueOf(startTs))
              .replace("{end}", String.valueOf(endTs));

      log.debug(
          "Fetching Yahoo Finance daily prices for {} (startTs={}, endTs={}, url={})",
          ticker.getTickerSymbol(),
          startTs,
          endTs,
          url);

      try {
        String json = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
        List<DailyPrice> parsedPrices = YahooResponseParser.parse(json, ticker);
        log.debug(
            "Fetched and parsed {} daily price records from Yahoo Finance for {}",
            parsedPrices.size(),
            ticker.getTickerSymbol());
        return parsedPrices;
      } catch (RestClientException | IOException exception) {
        log.error(
            "Failed to download Yahoo Finance data for {}: {}",
            ticker.getTickerSymbol(),
            exception.getMessage());
        return Collections.emptyList();
      }
    } else {
      log.warn("Cannot fetch Yahoo Finance daily prices: ticker is null.");
      return Collections.emptyList();
    }
  }
}
