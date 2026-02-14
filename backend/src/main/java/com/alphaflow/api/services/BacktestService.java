package com.alphaflow.api.services;

import com.alphaflow.api.dtos.BacktestSignalDTO;
import com.alphaflow.backtest.entities.BacktestSignal;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.repositories.BacktestSignalRepository;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.exceptions.ResourceNotFoundException;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class BacktestService {

    private final BacktestSignalRepository backtestSignalRepository;
    private final TickerRepository tickerRepository;

    public BacktestService(BacktestSignalRepository backtestSignalRepository, TickerRepository tickerRepository) {
        this.backtestSignalRepository = backtestSignalRepository;
        this.tickerRepository = tickerRepository;
    }

    public Map<String, List<BacktestSignalDTO>> getSignalsByTicker(String symbol) {
        Ticker ticker = tickerRepository.findByTickerSymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

        List<TradeSignal> relevantActions = List.of(TradeSignal.ENTER_LONG, TradeSignal.ENTER_SHORT, TradeSignal.EXIT);

        return backtestSignalRepository.findByTickerAndActionIn(ticker, relevantActions).stream()
                .map(this::mapToSignalDTO)
                .collect(Collectors.groupingBy(BacktestSignalDTO::strategyName));
    }

    private BacktestSignalDTO mapToSignalDTO(BacktestSignal signal) {
        return BacktestSignalDTO.builder()
                .strategyName(signal.getStrategy().getName())
                .signalDate(signal.getSignalDate())
                .action(signal.getAction())
                .build();
    }
}
