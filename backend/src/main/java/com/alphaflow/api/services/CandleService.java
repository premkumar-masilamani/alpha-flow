package com.alphaflow.api.services;

import com.alphaflow.api.dtos.CandleDTO;
import com.alphaflow.api.mappers.CandleMapper;
import com.alphaflow.infrastructure.repositories.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CandleService {

    private static final Logger log = LoggerFactory.getLogger(CandleService.class);

    private final CandleRepository candleRepository;

    public CandleService(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    public List<CandleDTO> getCandlesByTickerName(String tickerName) {
        log.debug("Fetching candles for ticker: {}", tickerName);
        return candleRepository.findAllByTickerNameWithTicker(tickerName)
                .stream()
                .map(CandleMapper::toDTO)
                .toList();
    }
}
