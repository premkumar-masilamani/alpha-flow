package com.alphaflow.infrastructure.persistence.mappers;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.infrastructure.persistence.entities.Ticker;

public class TickerMapper {
    public static TickerDTO toDTO(Ticker tickerEntity) {
        return new TickerDTO(
                tickerEntity.getTickerId(),
                tickerEntity.getTickerSymbol(),
                tickerEntity.getTickerName(),
                tickerEntity.getTickerDate().toString(),
                tickerEntity.isActive());
    }
}