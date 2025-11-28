package com.prem.ta.dtos;

public record TickerDto(
        Long tickerId,
        String symbol,
        String name,
        String startDate
) {
}
