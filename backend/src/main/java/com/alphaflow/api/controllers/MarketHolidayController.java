package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.api.services.MarketHolidayService;
import com.alphaflow.persistence.enums.Country;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/markets")
@Slf4j
public class MarketHolidayController {

  private final MarketHolidayService marketHolidayService;

  public MarketHolidayController(MarketHolidayService marketHolidayService) {
    this.marketHolidayService = marketHolidayService;
  }

  @GetMapping("/holidays")
  public List<MarketHolidayDto> getHolidays(
      @RequestParam(required = false) Country country,
      @RequestParam(required = false) Integer year) {
    log.info("Request to get market holidays for country: {}, year: {}", country, year);
    return marketHolidayService.getHolidays(country, year);
  }
}
