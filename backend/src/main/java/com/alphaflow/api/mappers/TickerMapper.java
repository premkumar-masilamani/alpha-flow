package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.infrastructure.entities.Ticker;

public class TickerMapper {
    public static TickerDTO toDTO(Ticker tickerEntity) {
        return TickerDTO.builder()
                .tickerId(tickerEntity.getTickerId())
                .tickerSymbol(tickerEntity.getTickerSymbol())
                .tickerName(tickerEntity.getTickerName())
                .tickerType(tickerEntity.getTickerType().name())
                .build();
    }
}