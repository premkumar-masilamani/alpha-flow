package com.alphaflow.api.services;

import com.alphaflow.api.dtos.DailyCandleDataDTO;
import com.alphaflow.api.mappers.DailyCandleDataMapper;
import com.alphaflow.infrastructure.repositories.DailyCandleDataRepository;
import com.alphaflow.infrastructure.repositories.WeeklyCandleDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DailyCandleDataService {

    private static final Logger log = LoggerFactory.getLogger(DailyCandleDataService.class);

    private final DailyCandleDataRepository dailyCandleDataRepository;
    private final WeeklyCandleDataRepository weeklyCandleDataRepository;

    public DailyCandleDataService(
            DailyCandleDataRepository dailyCandleDataRepository,
            WeeklyCandleDataRepository weeklyCandleDataRepository
    ) {
        this.dailyCandleDataRepository = dailyCandleDataRepository;
        this.weeklyCandleDataRepository = weeklyCandleDataRepository;
    }

    public List<DailyCandleDataDTO> getDailyCandleDataByTickerName(String tickerName) {
        log.debug("Fetching daily candle data for ticker: {}", tickerName);
        return dailyCandleDataRepository.findLatestByTickerName(tickerName, PageRequest.of(0, 180))
                .stream()
                .sorted(Comparator.comparing(com.alphaflow.infrastructure.entities.DailyCandleData::getCandleDataDate))
                .map(DailyCandleDataMapper::toDTO)
                .toList();
    }

    public List<DailyCandleDataDTO> getWeeklyCandleDataByTickerName(String tickerName) {
        log.debug("Fetching weekly candle data for ticker: {}", tickerName);
        return weeklyCandleDataRepository.findLatestByTickerName(tickerName, PageRequest.of(0, 180))
                .stream()
                .sorted(Comparator.comparing(com.alphaflow.infrastructure.entities.WeeklyCandleData::getCandleDataDate))
                .map(DailyCandleDataMapper::toDTO)
                .toList();
    }
}
