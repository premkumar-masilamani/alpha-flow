package com.alphaflow.api.services;

import com.alphaflow.api.dtos.CandleBarDTO;
import com.alphaflow.api.mappers.CandleBarMapper;
import com.alphaflow.infrastructure.repositories.CandleBarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CandleBarService {

    private static final Logger log = LoggerFactory.getLogger(CandleBarService.class);

    private final CandleBarRepository candleBarRepository;

    public CandleBarService(CandleBarRepository candleBarRepository) {
        this.candleBarRepository = candleBarRepository;
    }

    public List<CandleBarDTO> getCandleBarsByTickerName(String tickerName) {
        log.debug("Fetching candle bars for ticker: {}", tickerName);
        return candleBarRepository.findAllByTickerNameWithTicker(tickerName)
                .stream()
                .map(CandleBarMapper::toDTO)
                .toList();
    }
}
