package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.DailyPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DailyPriceController {

    private static final Logger log = LoggerFactory.getLogger(DailyPriceController.class);

    private final DailyPriceService dailyPriceService;

    public DailyPriceController(DailyPriceService dailyPriceService) {
        this.dailyPriceService = dailyPriceService;
    }

    @GetMapping("/tickers/{symbol}/data")
    public List<OhlcvDTO> getCandleDataForTicker(@PathVariable String symbol) {
        log.info("Request to get daily data for ticker: {}", symbol);
        return dailyPriceService.getDailyPriceByTickerName(symbol);
    }
}
