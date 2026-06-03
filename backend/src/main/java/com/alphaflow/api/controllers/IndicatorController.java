package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.api.utils.APIUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class IndicatorController {

  private static final Logger log = LoggerFactory.getLogger(IndicatorController.class);
  private final IndicatorService indicatorService;

  public IndicatorController(IndicatorService indicatorService) {
    this.indicatorService = indicatorService;
  }

  @GetMapping("/indicators")
  public List<IndicatorConfigDTO> getConfiguredIndicators() {
    log.info("Request to get configured indicators");
    return indicatorService.getConfiguredIndicators();
  }

  @GetMapping("/tickers/{symbol}/indicators")
  public List<IndicatorSeriesDTO> getIndicatorSeries(
      @PathVariable String symbol,
      @RequestParam String timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) Integer size) {

    log.info(
        "Request to get {} indicators for ticker: {}, page: {}, size: {}",
        timeframe,
        symbol,
        page,
        size);

    return indicatorService.getIndicatorSeries(
        symbol, APIUtil.parseTimeframe(timeframe), page, size);
  }
}
