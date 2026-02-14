package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.CandleDataDTO;
import com.alphaflow.api.services.CandleDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CandleDataController {

    private static final Logger log = LoggerFactory.getLogger(CandleDataController.class);

    private final CandleDataService candleDataService;

    public CandleDataController(CandleDataService candleDataService) {
        this.candleDataService = candleDataService;
    }

    @GetMapping("/tickers/{symbol}/data")
    public List<CandleDataDTO> getCandleDataForTicker(@PathVariable String symbol) {
        log.info("Request to get candle data for ticker: {}", symbol);
        return candleDataService.getCandleDataByTickerName(symbol);
    }
}
