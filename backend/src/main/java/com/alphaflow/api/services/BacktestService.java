package com.alphaflow.api.services;

import com.alphaflow.api.dtos.BacktestTradeDTO;
import com.alphaflow.backtest.entities.BacktestTrade;
import com.alphaflow.backtest.repositories.BacktestTradeRepository;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.exceptions.ResourceNotFoundException;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BacktestService {

    private final BacktestTradeRepository backtestTradeRepository;
    private final TickerRepository tickerRepository;

    public BacktestService(BacktestTradeRepository backtestTradeRepository, TickerRepository tickerRepository) {
        this.backtestTradeRepository = backtestTradeRepository;
        this.tickerRepository = tickerRepository;
    }

    public List<BacktestTradeDTO> getTradesByTicker(String symbol) {
        Ticker ticker = tickerRepository.findByTickerSymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

        return backtestTradeRepository.findByTicker(ticker).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private BacktestTradeDTO mapToDTO(BacktestTrade trade) {
        return BacktestTradeDTO.builder()
                .backtestTradeId(trade.getBacktestTradeId())
                .strategyName(trade.getStrategyName())
                .side(trade.getSide())
                .entryDate(trade.getEntryDate())
                .entryPrice(trade.getEntryPrice())
                .exitDate(trade.getExitDate())
                .exitPrice(trade.getExitPrice())
                .quantity(trade.getQuantity())
                .pnl(trade.getPnl())
                .pnlPct(trade.getPnlPct())
                .holdingBars(trade.getHoldingBars())
                .build();
    }
}
