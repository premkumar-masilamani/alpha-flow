package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.WeeklyPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WeeklyPriceController {

    private static final Logger log = LoggerFactory.getLogger(WeeklyPriceController.class);

    private final WeeklyPriceService weeklyPriceService;

    public WeeklyPriceController(WeeklyPriceService weeklyPriceService) {
        this.weeklyPriceService = weeklyPriceService;
    }

    @GetMapping("/tickers/{symbol}/weekly-data")
    public List<OhlcvDTO> getWeeklyDataForTicker(@PathVariable String symbol) {
        log.info("Request to get weekly data for ticker: {}", symbol);
        return weeklyPriceService.getWeeklyPriceByTickerName(symbol);
    }
}
