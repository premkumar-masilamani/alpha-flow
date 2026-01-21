package com.alphaflow.api.services;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.api.exceptions.ResourceNotFoundException;
import com.alphaflow.common.repositories.TickerRepository;
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
                .map(ticker -> new TickerDTO(
                        ticker.getTickerId(),
                        ticker.getTickerSymbol(),
                        ticker.getTickerName(),
                        ticker.getTickerDate().toString(),
                        ticker.isActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));
    }

    public List<TickerDTO> getAllTickers() {
        return tickerRepository.findAll()
                .stream()
                .map(ticker -> new TickerDTO(
                        ticker.getTickerId(),
                        ticker.getTickerSymbol(),
                        ticker.getTickerName(),
                        ticker.getTickerDate().toString(),
                        ticker.isActive()))
                .toList();
    }
}
