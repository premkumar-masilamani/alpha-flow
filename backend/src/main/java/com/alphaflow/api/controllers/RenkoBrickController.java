package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.RenkoResponseDTO;
import com.alphaflow.api.services.RenkoBrickService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RenkoBrickController {

    private static final Logger log = LoggerFactory.getLogger(RenkoBrickController.class);

    private final RenkoBrickService renkoBrickService;

    public RenkoBrickController(RenkoBrickService renkoBrickService) {
        this.renkoBrickService = renkoBrickService;
    }

    @GetMapping("/tickers/{symbol}/renko")
    public RenkoResponseDTO getRenkoBricksForTicker(@PathVariable String symbol) {
        log.info("Request to get renko bricks for ticker: {}", symbol);
        return renkoBrickService.getRenkoBricks(symbol);
    }
}
