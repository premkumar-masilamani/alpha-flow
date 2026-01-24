package com.alphaflow.domain.service;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.domain.exceptions.ResourceNotFoundException;
import com.alphaflow.infrastructure.persistence.mappers.TickerMapper;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TickerService {

    private final TickerRepository tickerRepository;

    public TickerService(TickerRepository tickerRepository) {
        this.tickerRepository = tickerRepository;
    }

    public TickerDTO getTickerBySymbol(String symbol) {
        return tickerRepository.findByTickerSymbol(symbol)
                .map(TickerMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));
    }


    public List<TickerDTO> getAllTickers() {
        return tickerRepository.findByIsActiveTrue()
                .stream()
                .map(TickerMapper::toDTO)
                .toList();
    }
}
