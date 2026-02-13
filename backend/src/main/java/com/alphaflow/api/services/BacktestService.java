package com.alphaflow.api.services;

import com.alphaflow.api.dtos.BacktestSignalsDTO;
import com.alphaflow.backtest.entities.BacktestSignal;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.repositories.BacktestSignalsRepository;
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

    private final BacktestSignalsRepository backtestSignalsRepository;
    private final TickerRepository tickerRepository;

    public BacktestService(BacktestSignalsRepository backtestSignalsRepository, TickerRepository tickerRepository) {
        this.backtestSignalsRepository = backtestSignalsRepository;
        this.tickerRepository = tickerRepository;
    }

    public List<BacktestSignalsDTO> getSignalsByTicker(String symbol) {
        Ticker ticker = tickerRepository.findByTickerSymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

        List<TradeSignal> relevantActions = List.of(TradeSignal.ENTER_LONG, TradeSignal.ENTER_SHORT, TradeSignal.EXIT);

        return backtestSignalsRepository.findByTickerAndActionIn(ticker, relevantActions).stream()
                .map(this::mapToSignalDTO)
                .collect(Collectors.toList());
    }

    private BacktestSignalsDTO mapToSignalDTO(BacktestSignal signal) {
        return BacktestSignalsDTO.builder()
                .strategyName(signal.getStrategy().getName())
                .signalDate(signal.getSignalDate())
                .action(signal.getAction())
                .build();
    }
}
