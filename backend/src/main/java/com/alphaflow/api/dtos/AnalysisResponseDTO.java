package com.alphaflow.api.dtos;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record AnalysisResponseDTO(
        String symbol,
        LocalDate priceDate,
        String emaSignal,
        String emaValue,
        String macdSignal,
        String macdValue,
        String stochasticSignal,
        String stochasticValue,
        String rsiSignal,
        String rsiValue,
        String volumeSignal,
        String volumeValue,
        String overallSignal
) {}
