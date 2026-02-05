package com.alphaflow.api.dtos;

import com.alphaflow.backtest.enums.PositionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTradeDTO {
    private Long backtestTradeId;
    private String strategyName;
    private PositionType side;
    private LocalDate entryDate;
    private BigDecimal entryPrice;
    private LocalDate exitDate;
    private BigDecimal exitPrice;
    private BigDecimal quantity;
    private BigDecimal pnl;
    private BigDecimal pnlPct;
    private Integer holdingBars;
}
