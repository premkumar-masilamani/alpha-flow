package com.alphaflow.api.services;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.api.mappers.MarketDataMapper;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final MarketDataRepository marketDataRepository;

    public MarketDataService(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    public List<MarketDataDTO> getMarketDataByTickerName(String tickerName) {
        log.debug("Fetching market data for ticker: {}", tickerName);
        return marketDataRepository.findAllByTickerNameWithTicker(tickerName)
                .stream()
                .map(MarketDataMapper::toDTO)
                .toList();
    }
}
