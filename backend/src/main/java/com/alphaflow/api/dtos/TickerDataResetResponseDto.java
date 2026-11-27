package com.alphaflow.api.dtos;

public record TickerDataResetResponseDto(String symbol, boolean cleared, String message) {}
