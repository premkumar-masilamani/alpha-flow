package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.api.services.MarketHolidayService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
  public List<MarketHolidayDto> getHolidays() {
    log.info("Request to get market holidays for the current year");
    return marketHolidayService.getHolidays();
  }
}
