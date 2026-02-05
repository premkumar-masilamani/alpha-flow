package com.alphaflow.api.dtos;

import com.alphaflow.backtest.enums.TradeSignal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestSignalDTO {
    private Long backtestSignalId;
    private String strategyName;
    private LocalDate signalDate;
    private LocalDate executeDate;
    private TradeSignal action;
    private String signalData;
}
