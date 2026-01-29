package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.api.dtos.RenkoResponseDTO;
import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.api.services.MarketDataService;
import com.alphaflow.api.services.RenkoDataService;
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
    private final RenkoDataService renkoDataService;

    public TickerController(TickerService tickerService, MarketDataService marketDataService, RenkoDataService renkoDataService) {
        this.tickerService = tickerService;
        this.marketDataService = marketDataService;
        this.renkoDataService = renkoDataService;
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

    @GetMapping("/tickers/{symbol}/renko")
    public RenkoResponseDTO getRenkoDataForTicker(@PathVariable String symbol) {
        return renkoDataService.getRenkoData(symbol);
    }
}
