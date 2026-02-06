package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.BacktestSignalDTO;
import com.alphaflow.api.services.BacktestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BacktestController {

    private static final Logger log = LoggerFactory.getLogger(BacktestController.class);

    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    @GetMapping("/tickers/{symbol}/signals")
    public List<BacktestSignalDTO> getSignalsForTicker(@PathVariable String symbol) {
        log.info("Request to get backtest signals for ticker: {}", symbol);
        return backtestService.getSignalsByTicker(symbol);
    }
}
