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

  private static final Logger logger = LoggerFactory.getLogger(IndicatorController.class);
  private final IndicatorService indicatorService;

  public IndicatorController(IndicatorService indicatorService) {
    this.indicatorService = indicatorService;
  }

  @GetMapping("/indicators")
  public List<IndicatorConfigDTO> getConfiguredIndicators() {
    logger.info("Request to get configured indicators");
    return indicatorService.getConfiguredIndicators();
  }

  @GetMapping("/tickers/{symbol}/indicators")
  public List<IndicatorSeriesDTO> getIndicatorSeries(
      @PathVariable String symbol,
      @RequestParam String timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.max(1, Math.min(size, 1000));
    logger.info(
        "Request to get {} indicators for ticker: {}, page: {}, size: {}",
        timeframe,
        symbol,
        page,
        finalSize);

    return indicatorService.getIndicatorSeries(
        symbol, APIUtil.parseTimeframe(timeframe), page, finalSize);
  }
}
