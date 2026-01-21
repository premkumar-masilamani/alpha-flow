package com.prem.ta.services;

import com.prem.ta.dtos.TickerDTO;
import com.prem.ta.exceptions.ResourceNotFoundException;
import com.prem.ta.repositories.TickerRepository;
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
