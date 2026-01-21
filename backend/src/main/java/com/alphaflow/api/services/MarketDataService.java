package com.alphaflow.api.services;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.common.mappers.MarketDataMapper;
import com.alphaflow.common.repositories.MarketDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MarketDataService {

    private final MarketDataRepository marketDataRepository;

    public MarketDataService(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    public List<MarketDataDTO> getMarketDataByTickerName(String tickerName) {
        return marketDataRepository.findAllByTickerNameWithTicker(tickerName)
                .stream()
                .map(MarketDataMapper::toDTO)
                .toList();
    }
}
