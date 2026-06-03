package com.alphaflow.api.services;


import com.alphaflow.api.dtos.TickerDTO;

import com.alphaflow.api.mappers.TickerMapper;

import com.alphaflow.persistence.exceptions.ResourceNotFoundException;

import com.alphaflow.persistence.repositories.TickerRepository;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service

@Transactional(readOnly = true)

public class TickerService {


  private static final Logger log = LoggerFactory.getLogger(TickerService.class);


  private final TickerRepository tickerRepository;


  public TickerService(TickerRepository tickerRepository) {

    this.tickerRepository = tickerRepository;

  }


  public TickerDTO getTickerBySymbol(String symbol) {

    log.debug("Fetching ticker for symbol: {}", symbol);

    return tickerRepository.findByTickerSymbol(symbol)

        .map(TickerMapper::toDTO)

        .orElseThrow(() -> {

          log.warn("Ticker not found for symbol: {}", symbol);

          return new ResourceNotFoundException("Ticker not found: " + symbol);

        });

  }



  public List<TickerDTO> getAllTickers() {

    return tickerRepository.findByIsActiveTrue()

        .stream()

        .map(TickerMapper::toDTO)

        .toList();

  }

}

