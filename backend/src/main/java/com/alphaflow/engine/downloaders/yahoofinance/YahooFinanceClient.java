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

  private final YahooFinanceConfig config;
  private final RestClient restClient;

  @Autowired
  public YahooFinanceClient(
      YahooFinanceConfig config, @Autowired(required = false) RestClient.Builder restClientBuilder) {
    this.config = config;
    this.restClient =
        (restClientBuilder != null ? restClientBuilder : RestClient.builder())
            .defaultHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .defaultHeader("Accept", "application/json")
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
        sleep(config.getDelayMilliseconds());
        return YahooResponseParser.parse(json, ticker);
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

  private void sleep(long millis) {
    if (millis > 0) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        log.warn("Rate-limit sleep interrupted during Yahoo Finance download: {}", exception.getMessage());
      }
    }
  }
}
