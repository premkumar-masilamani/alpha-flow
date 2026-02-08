package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.CandleDTO;
import com.alphaflow.api.services.CandleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CandleController {

    private static final Logger log = LoggerFactory.getLogger(CandleController.class);

    private final CandleService candleService;

    public CandleController(CandleService candleService) {
        this.candleService = candleService;
    }

    @GetMapping("/tickers/{symbol}/candles")
    public List<CandleDTO> getCandlesForTicker(@PathVariable String symbol) {
        log.info("Request to get candles for ticker: {}", symbol);
        return candleService.getCandlesByTickerName(symbol);
    }
}
