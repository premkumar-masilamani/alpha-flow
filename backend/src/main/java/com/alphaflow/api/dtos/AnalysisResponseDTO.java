package com.alphaflow.api.dtos;


import java.time.LocalDate;

import lombok.Builder;


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

