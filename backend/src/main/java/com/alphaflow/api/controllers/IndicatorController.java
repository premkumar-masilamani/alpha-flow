package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.persistence.enums.Timeframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IndicatorController {

    private static final Logger log = LoggerFactory.getLogger(IndicatorController.class);

    private final IndicatorService indicatorService;

    public IndicatorController(IndicatorService indicatorService) {
        this.indicatorService = indicatorService;
    }

    /** Discovery: the configured indicator matrix, so clients can build controls from config. */
    @GetMapping("/indicators")
    public List<IndicatorConfigDTO> getConfiguredIndicators() {
        log.info("Request to get configured indicators");
        return indicatorService.getConfiguredIndicators();
    }

    /** All configured indicators for a ticker on a timeframe (e.g. {@code ?timeframe=DAILY}). */
    @GetMapping("/tickers/{symbol}/indicators")
    public List<IndicatorSeriesDTO> getIndicatorSeries(@PathVariable String symbol,
                                                       @RequestParam String timeframe) {
        log.info("Request to get {} indicators for ticker: {}", timeframe, symbol);
        return indicatorService.getIndicatorSeries(symbol, parseTimeframe(timeframe));
    }

    private static Timeframe parseTimeframe(String timeframe) {
        try {
            return Timeframe.valueOf(timeframe.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid timeframe: '" + timeframe + "' (expected DAILY or WEEKLY)");
        }
    }
}
