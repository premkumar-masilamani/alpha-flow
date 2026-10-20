package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.api.services.IntradayPriceService;
import com.alphaflow.api.services.LiveQuoteService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class IntradayPriceController {

  private final IntradayPriceService intradayPriceService;
  private final LiveQuoteService liveQuoteService;
  private final TickerService tickerService;

  public IntradayPriceController(
      IntradayPriceService intradayPriceService,
      LiveQuoteService liveQuoteService,
      TickerService tickerService) {
    this.intradayPriceService = intradayPriceService;
    this.liveQuoteService = liveQuoteService;
    this.tickerService = tickerService;
  }

  @GetMapping("/tickers/{symbol}/candle")
  public List<OhlcvDto> getIntradayPriceDataForTicker(
      @PathVariable String symbol,
      @RequestParam IntradayTimeframe timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {

    int finalSize = Math.clamp(size, 1, 1000);
    log.info(
        "Request to get {} intraday data for ticker: {}, page: {}, size: {}, endDate: {}",
        timeframe,
        symbol,
        page,
        finalSize,
        endDate);

    Ticker ticker = tickerService.getTicker(symbol);

    if (timeframe == IntradayTimeframe._15M) {
      LocalDate resolvedEndDate =
          (endDate != null) ? endDate : LocalDate.now(ticker.getCountry().getZoneId());
      return intradayPriceService.getIntradayPrice(
          ticker, timeframe, resolvedEndDate, page, finalSize);
    } else {
      log.error("Unsupported timeframe for intraday price data: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  @GetMapping("/tickers/{symbol}/quote")
  public QuoteDto getQuoteForTicker(@PathVariable String symbol) {
    log.info("Request to get quote for ticker: {}", symbol);
    Ticker ticker = tickerService.getTicker(symbol);
    return liveQuoteService.getQuote(ticker);
  }
}
