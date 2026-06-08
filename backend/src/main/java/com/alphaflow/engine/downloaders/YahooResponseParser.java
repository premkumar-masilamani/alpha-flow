package com.alphaflow.engine.downloaders;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class YahooResponseParser {

  public List<DailyPrice> parse(String jsonString, Ticker ticker) throws IOException {
    log.debug("Parsing Yahoo Finance JSON response for ticker: {}", ticker.getTickerSymbol());

    if (jsonString == null || jsonString.isBlank()) {
      log.warn("Yahoo Finance response was null or empty for ticker: {}", ticker.getTickerSymbol());
      return List.of();
    }

    ObjectMapper objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    YahooResponse response = objectMapper.readValue(jsonString, YahooResponse.class);

    if (response == null
        || response.chart() == null
        || response.chart().result() == null
        || response.chart().result().isEmpty()) {
      log.warn(
          "Yahoo Finance JSON structure chart/result is missing for ticker: {}",
          ticker.getTickerSymbol());
      return List.of();
    }

    YahooResult result = response.chart().result().get(0);
    if (result.timestamp() == null
        || result.indicators() == null
        || result.indicators().quote() == null
        || result.indicators().quote().isEmpty()) {
      log.warn(
          "Yahoo Finance JSON structure timestamp/quote is missing for ticker: {}",
          ticker.getTickerSymbol());
      return List.of();
    }

    YahooQuote quote = result.indicators().quote().get(0);
    if (quote.open() == null
        || quote.high() == null
        || quote.low() == null
        || quote.close() == null
        || quote.volume() == null) {
      log.warn(
          "Yahoo Finance JSON structure quote fields are missing for ticker: {}",
          ticker.getTickerSymbol());
      return List.of();
    }

    int count =
        Math.min(
            result.timestamp().size(),
            Math.min(
                Math.min(quote.open().size(), quote.high().size()),
                Math.min(
                    quote.low().size(), Math.min(quote.close().size(), quote.volume().size()))));

    List<DailyPrice> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      BigDecimal open = quote.open().get(i);
      BigDecimal high = quote.high().get(i);
      BigDecimal low = quote.low().get(i);
      BigDecimal close = quote.close().get(i);
      BigDecimal volume = quote.volume().get(i);

      if (open == null || high == null || low == null || close == null || volume == null) {
        log.debug(
            "Skipped null price bar values for ticker: {} at index: {}",
            ticker.getTickerSymbol(),
            i);
        continue;
      }

      // Discard invalid (zero or negative) price bars (e.g. during market transition / incomplete
      // day downloads)
      if (open.compareTo(BigDecimal.ZERO) <= 0
          || high.compareTo(BigDecimal.ZERO) <= 0
          || low.compareTo(BigDecimal.ZERO) <= 0
          || close.compareTo(BigDecimal.ZERO) <= 0) {
        log.warn(
            "Discarded invalid/zero/negative price bar for ticker: {} at index: {} (open={}, high={}, low={}, close={})",
            ticker.getTickerSymbol(),
            i,
            open,
            high,
            low,
            close);
        continue;
      }

      LocalDate date =
          Instant.ofEpochSecond(result.timestamp().get(i)).atZone(ZoneId.of("UTC")).toLocalDate();

      list.add(
          DailyPrice.builder()
              .ticker(ticker)
              .priceDate(date)
              .priceOpen(open)
              .priceHigh(high)
              .priceLow(low)
              .priceClose(close)
              .volume(volume)
              .build());
    }

    log.info(
        "Successfully parsed {} daily price records for ticker {}",
        list.size(),
        ticker.getTickerSymbol());
    return list;
  }

  private record YahooResponse(YahooChart chart) {}

  private record YahooChart(List<YahooResult> result) {}

  private record YahooResult(List<Long> timestamp, YahooIndicators indicators) {}

  private record YahooIndicators(List<YahooQuote> quote) {}

  private record YahooQuote(
      List<BigDecimal> open,
      List<BigDecimal> high,
      List<BigDecimal> low,
      List<BigDecimal> close,
      List<BigDecimal> volume) {}
}
