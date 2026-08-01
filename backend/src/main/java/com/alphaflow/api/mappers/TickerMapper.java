package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.persistence.entities.Ticker;

public class TickerMapper {

  public static TickerDto toDto(Ticker tickerEntity) {
    return TickerDto.builder()
        .tickerId(tickerEntity.getTickerId())
        .tickerSymbol(tickerEntity.getTickerSymbol())
        .tickerName(tickerEntity.getTickerName())
        .build();
  }
}
