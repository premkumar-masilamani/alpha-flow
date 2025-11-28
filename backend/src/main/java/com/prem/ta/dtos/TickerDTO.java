package com.prem.ta.dtos;

public record TickerDTO(
        Long tickerId,
        String symbol,
        String name,
        String startDate
) {
}
