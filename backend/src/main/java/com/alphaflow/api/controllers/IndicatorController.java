package com.alphaflow.api.controllers;


import com.alphaflow.api.dtos.IndicatorConfigDTO;

import com.alphaflow.api.dtos.IndicatorSeriesDTO;

import com.alphaflow.api.services.IndicatorService;

import com.alphaflow.persistence.enums.Timeframe;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController

@RequestMapping("/api")

public class IndicatorController {


  private static final Logger log = LoggerFactory.getLogger(IndicatorController.class);


  private final IndicatorService indicatorService;


  public IndicatorController(IndicatorService indicatorService) {

    this.indicatorService = indicatorService;

  }


  private static Timeframe parseTimeframe(String timeframe) {

    try {

      return Timeframe.valueOf(timeframe.trim().toUpperCase());

    } catch (IllegalArgumentException | NullPointerException e) {

      throw new IllegalArgumentException("Invalid timeframe: '" + timeframe + "' (expected DAILY or WEEKLY)");

    }

  }


  /**

   * Discovery: the configured indicator matrix, so clients can build controls from config.

   */

  @GetMapping("/indicators")

  public List<IndicatorConfigDTO> getConfiguredIndicators() {

    log.info("Request to get configured indicators");

    return indicatorService.getConfiguredIndicators();

  }


  /**

   * All configured indicators for a ticker on a timeframe (e.g. {@code ?timeframe=DAILY}).

   */

  @GetMapping("/tickers/{symbol}/indicators")

  public List<IndicatorSeriesDTO> getIndicatorSeries(@PathVariable String symbol,

                             @RequestParam String timeframe,

                             @RequestParam(defaultValue = "0") int page,

                             @RequestParam(required = false) Integer size) {

    log.info("Request to get {} indicators for ticker: {}, page: {}, size: {}", timeframe, symbol, page, size);

    return indicatorService.getIndicatorSeries(symbol, parseTimeframe(timeframe), page, size);

  }

}

