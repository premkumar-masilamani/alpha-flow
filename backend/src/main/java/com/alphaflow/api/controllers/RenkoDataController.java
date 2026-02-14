package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.RenkoDataResponseDTO;
import com.alphaflow.api.services.RenkoDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RenkoDataController {

    private static final Logger log = LoggerFactory.getLogger(RenkoDataController.class);

    private final RenkoDataService renkoDataService;

    public RenkoDataController(RenkoDataService renkoDataService) {
        this.renkoDataService = renkoDataService;
    }

    @GetMapping("/tickers/{symbol}/renko")
    public RenkoDataResponseDTO getRenkoDataForTicker(@PathVariable String symbol) {
        log.info("Request to get renko data for ticker: {}", symbol);
        return renkoDataService.getRenkoData(symbol);
    }
}
