package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.api.services.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MarketDataController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataController.class);

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/tickers/{symbol}/data")
    public List<MarketDataDTO> getMarketDataForTicker(@PathVariable String symbol) {
        log.info("Request to get market data for ticker: {}", symbol);
        return marketDataService.getMarketDataByTickerName(symbol);
    }
}
