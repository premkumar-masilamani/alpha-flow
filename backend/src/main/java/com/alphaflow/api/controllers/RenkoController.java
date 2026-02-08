package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.RenkoResponseDTO;
import com.alphaflow.api.services.RenkoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RenkoController {

    private static final Logger log = LoggerFactory.getLogger(RenkoController.class);

    private final RenkoService renkoService;

    public RenkoController(RenkoService renkoService) {
        this.renkoService = renkoService;
    }

    @GetMapping("/tickers/{symbol}/renko")
    public RenkoResponseDTO getRenkoForTicker(@PathVariable String symbol) {
        log.info("Request to get renko data for ticker: {}", symbol);
        return renkoService.getRenko(symbol);
    }
}
