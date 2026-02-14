package com.alphaflow.api.services;

import com.alphaflow.api.dtos.CandleDataDTO;
import com.alphaflow.api.mappers.CandleDataMapper;
import com.alphaflow.infrastructure.repositories.CandleDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CandleDataService {

    private static final Logger log = LoggerFactory.getLogger(CandleDataService.class);

    private final CandleDataRepository candleDataRepository;

    public CandleDataService(CandleDataRepository candleDataRepository) {
        this.candleDataRepository = candleDataRepository;
    }

    public List<CandleDataDTO> getCandleDataByTickerName(String tickerName) {
        log.debug("Fetching candle data for ticker: {}", tickerName);
        return candleDataRepository.findAllByTickerNameWithTicker(tickerName)
                .stream()
                .map(CandleDataMapper::toDTO)
                .toList();
    }
}
