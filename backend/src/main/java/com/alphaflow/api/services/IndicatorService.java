package com.alphaflow.api.services;

import com.alphaflow.api.configs.ApiProperties;
import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.mappers.IndicatorMapper;
import com.alphaflow.engine.configs.IndicatorProperties;
import com.alphaflow.engine.configs.IndicatorProperties.IndicatorDefinition;
import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorValueRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Serves the indicator discovery matrix and the per-ticker, per-timeframe indicator series.
 * <p>
 * The series window mirrors the price endpoint: indicators are returned for the same date range as
 * the most recent {@code window} candles, so chart overlays align exactly with the bars.
 */
@Service
@Transactional(readOnly = true)
public class IndicatorService {

    private static final Logger log = LoggerFactory.getLogger(IndicatorService.class);

    private final IndicatorProperties indicatorProperties;
    private final ApiProperties apiProperties;
    private final TickerRepository tickerRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final WeeklyPriceRepository weeklyPriceRepository;
    private final IndicatorValueRepository indicatorValueRepository;

    public IndicatorService(IndicatorProperties indicatorProperties,
                            ApiProperties apiProperties,
                            TickerRepository tickerRepository,
                            DailyPriceRepository dailyPriceRepository,
                            WeeklyPriceRepository weeklyPriceRepository,
                            IndicatorValueRepository indicatorValueRepository) {
        this.indicatorProperties = indicatorProperties;
        this.apiProperties = apiProperties;
        this.tickerRepository = tickerRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.weeklyPriceRepository = weeklyPriceRepository;
        this.indicatorValueRepository = indicatorValueRepository;
    }

    /** The configured indicator matrix across all timeframes. */
    public List<IndicatorConfigDTO> getConfiguredIndicators() {
        List<IndicatorConfigDTO> configs = new ArrayList<>();
        for (Timeframe timeframe : Timeframe.values()) {
            for (IndicatorDefinition definition : indicatorProperties.forTimeframe(timeframe)) {
                configs.add(IndicatorMapper.toConfigDTO(timeframe, definition));
            }
        }
        return configs;
    }

    /** All configured indicators for a ticker on a timeframe, over the configured window. */
    public List<IndicatorSeriesDTO> getIndicatorSeries(String symbol, Timeframe timeframe) {
        log.debug("Fetching {} indicators for ticker: {}", timeframe, symbol);
        if (!tickerRepository.existsByTickerSymbolIgnoreCase(symbol)) {
            log.warn("Ticker not found for symbol: {}", symbol);
            throw new ResourceNotFoundException("Ticker not found: " + symbol);
        }

        int window = apiProperties.windowFor(timeframe);
        List<LocalDate> recentDates = recentPriceDates(symbol, timeframe, window);
        if (recentDates.isEmpty()) {
            return List.of();
        }
        // recentDates is newest-first; its last element is the oldest bar within the window.
        LocalDate windowStart = recentDates.getLast();

        List<IndicatorValue> rows = indicatorValueRepository.findSeries(symbol, timeframe, windowStart);
        return IndicatorMapper.toSeries(rows);
    }

    private List<LocalDate> recentPriceDates(String symbol, Timeframe timeframe, int window) {
        PageRequest page = PageRequest.of(0, window);
        return timeframe == Timeframe.WEEKLY
                ? weeklyPriceRepository.findRecentPriceDates(symbol, page)
                : dailyPriceRepository.findRecentPriceDates(symbol, page);
    }
}
