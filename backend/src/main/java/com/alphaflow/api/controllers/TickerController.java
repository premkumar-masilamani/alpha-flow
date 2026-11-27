package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.api.services.TickerDataResetService;
import com.alphaflow.api.services.TickerService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class TickerController {

  private final TickerService tickerService;
  private final TickerDataResetService tickerDataResetService;

  public TickerController(
      TickerService tickerService, TickerDataResetService tickerDataResetService) {
    this.tickerService = tickerService;
    this.tickerDataResetService = tickerDataResetService;
  }

  @GetMapping("/tickers")
  public List<TickerDto> getAllTickers() {
    log.info("Request to get all tickers");
    return tickerService.getAllTickers();
  }

  @GetMapping("/tickers/{symbol}")
  public TickerDto getTickerBySymbol(@PathVariable String symbol) {
    log.debug("Request to get ticker by symbol: {}", symbol);
    return tickerService.getTickerBySymbol(symbol);
  }

  @DeleteMapping("/tickers/{symbol}/data")
  public ResponseEntity<Void> deleteTickerData(@PathVariable String symbol) {
    log.info("Request to clear data for ticker: {}", symbol);
    tickerDataResetService.resetTickerData(symbol);
    return ResponseEntity.ok().build();
  }
}
