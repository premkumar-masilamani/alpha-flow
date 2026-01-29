package com.alphaflow.api.services;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.infrastructure.exceptions.ResourceNotFoundException;
import com.alphaflow.api.mappers.TickerMapper;
import com.alphaflow.infrastructure.repositories.TickerRepository;
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
