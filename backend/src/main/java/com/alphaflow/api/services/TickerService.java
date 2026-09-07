package com.alphaflow.api.services;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.api.mappers.TickerMapper;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class TickerService {

  private final TickerRepository tickerRepository;

  public TickerService(TickerRepository tickerRepository) {
    this.tickerRepository = tickerRepository;
  }

  public Ticker getTicker(String symbol) {
    log.debug("Fetching ticker entity for symbol: {}", symbol);

    return tickerRepository
        .findByTickerSymbolIgnoreCase(symbol)
        .orElseThrow(
            () -> {
              log.warn("Ticker not found for symbol: {}", symbol);

              return new ResourceNotFoundException("Ticker not found: " + symbol);
            });
  }

  public TickerDto getTickerBySymbol(String symbol) {
    log.debug("Fetching ticker for symbol: {}", symbol);

    return TickerMapper.toDto(getTicker(symbol));
  }

  public List<TickerDto> getAllTickers() {
    return tickerRepository.findByIsActiveTrue().stream().map(TickerMapper::toDto).toList();
  }
}
