package com.alphaflow.api.dtos;

import com.alphaflow.persistence.enums.TradeAction;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record ASTAResponseDTO(
    String symbol,
    LocalDate priceDate,
    TradeAction emaSignal,
    String emaValue,
    TradeAction macdSignal,
    String macdValue,
    TradeAction stochasticSignal,
    String stochasticValue,
    TradeAction rsiSignal,
    String rsiValue,
    TradeAction volumeSignal,
    String volumeValue,
    TradeAction overallSignal) {}
