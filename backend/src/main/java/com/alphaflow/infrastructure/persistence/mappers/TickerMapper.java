package com.alphaflow.infrastructure.persistence.mappers;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.infrastructure.persistence.entities.Ticker;

public class TickerMapper {
    public static TickerDTO toDTO(Ticker tickerEntity) {
        return TickerDTO.builder()
                .tickerId(tickerEntity.getTickerId())
                .tickerSymbol(tickerEntity.getTickerSymbol())
                .tickerName(tickerEntity.getTickerName())
                .tickerDate(tickerEntity.getTickerDate().toString())
                .isActive(tickerEntity.isActive()).build();
    }
}