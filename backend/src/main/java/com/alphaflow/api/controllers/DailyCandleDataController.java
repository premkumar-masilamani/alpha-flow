package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.DailyCandleDataDTO;
import com.alphaflow.api.services.DailyCandleDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DailyCandleDataController {

    private static final Logger log = LoggerFactory.getLogger(DailyCandleDataController.class);

    private final DailyCandleDataService dailyCandleDataService;

    public DailyCandleDataController(DailyCandleDataService dailyCandleDataService) {
        this.dailyCandleDataService = dailyCandleDataService;
    }

    @GetMapping("/tickers/{symbol}/data")
    public List<DailyCandleDataDTO> getCandleDataForTicker(
            @PathVariable String symbol,
            @RequestParam(value = "timeframe", defaultValue = "daily") String timeframe
    ) {
        log.info("Request to get {} candle data for ticker: {}", timeframe, symbol);
        if ("weekly".equalsIgnoreCase(timeframe)) {
            return dailyCandleDataService.getWeeklyCandleDataByTickerName(symbol);
        } else {
            return dailyCandleDataService.getDailyCandleDataByTickerName(symbol);
        }
    }
}
