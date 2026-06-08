package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.api.utils.APIUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class IndicatorController {

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
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.clamp(size, size, 1000);
    log.info(
        "Request to get {} indicators for ticker: {}, page: {}, size: {}",
        timeframe,
        symbol,
        page,
        finalSize);

    return indicatorService.getIndicatorSeries(
        symbol, APIUtil.parseTimeframe(timeframe), page, finalSize);
  }
}
