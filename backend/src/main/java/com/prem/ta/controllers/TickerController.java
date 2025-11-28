package com.prem.ta.controllers;

import com.prem.ta.dtos.TickerDTO;
import com.prem.ta.dtos.TradeDataDTO;
import com.prem.ta.services.TickerService;
import com.prem.ta.services.TradeDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TickerController {

    private final TickerService tickerService;
    private final TradeDataService tradeDataService;

    public TickerController(TickerService tickerService,
                            TradeDataService tradeDataService) {
        this.tickerService = tickerService;
        this.tradeDataService = tradeDataService;
    }

    @GetMapping("/tickers")
    public List<TickerDTO> getAllTickers() {
        return tickerService.getAllTickers();
    }

    @GetMapping("/tickers/{symbol}")
    public TickerDTO getTickerBySymbol(@PathVariable String symbol) {
        return tickerService.getTickerBySymbol(symbol);
    }

    @GetMapping("/tickers/{symbol}/trades")
    public List<TradeDataDTO> getTradesForTicker(@PathVariable String symbol) {
        return tradeDataService.getTradesByTickerName(symbol);
    }
}
