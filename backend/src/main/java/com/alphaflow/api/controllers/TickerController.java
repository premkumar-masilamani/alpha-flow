package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.api.services.MarketDataService;
import com.alphaflow.api.services.TickerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TickerController {

    private final TickerService tickerService;
    private final MarketDataService marketDataService;

    public TickerController(TickerService tickerService, MarketDataService marketDataService) {
        this.tickerService = tickerService;
        this.marketDataService = marketDataService;
    }

    @GetMapping("/tickers")
    public List<TickerDTO> getAllTickers() {
        return tickerService.getAllTickers();
    }

    @GetMapping("/tickers/{symbol}")
    public TickerDTO getTickerBySymbol(@PathVariable String symbol) {
        return tickerService.getTickerBySymbol(symbol);
    }

    @GetMapping("/tickers/{symbol}/data")
    public List<MarketDataDTO> getMarketDataForTicker(@PathVariable String symbol) {
        return marketDataService.getMarketDataByTickerName(symbol);
    }
}
