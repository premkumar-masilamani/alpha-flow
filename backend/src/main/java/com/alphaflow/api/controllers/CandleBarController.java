package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.CandleBarDTO;
import com.alphaflow.api.services.CandleBarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CandleBarController {

    private static final Logger log = LoggerFactory.getLogger(CandleBarController.class);

    private final CandleBarService candleBarService;

    public CandleBarController(CandleBarService candleBarService) {
        this.candleBarService = candleBarService;
    }

    @GetMapping("/tickers/{symbol}/candles")
    public List<CandleBarDTO> getCandleBarsForTicker(@PathVariable String symbol) {
        log.info("Request to get candle bars for ticker: {}", symbol);
        return candleBarService.getCandleBarsByTickerName(symbol);
    }
}
