package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.DailyCandleDataDTO;
import com.alphaflow.infrastructure.entities.DailyCandleData;
import com.alphaflow.infrastructure.entities.WeeklyCandleData;

public class DailyCandleDataMapper {
    public static DailyCandleDataDTO toDTO(DailyCandleData candleEntity) {
        return DailyCandleDataDTO.builder()
                .candleDataDate(candleEntity.getCandleDataDate())
                .priceOpen(candleEntity.getPriceOpen())
                .priceHigh(candleEntity.getPriceHigh())
                .priceLow(candleEntity.getPriceLow())
                .priceClose(candleEntity.getPriceClose())
                .volume(candleEntity.getVolume())
                .build();
    }

    public static DailyCandleDataDTO toDTO(WeeklyCandleData weeklyEntity) {
        return DailyCandleDataDTO.builder()
                .candleDataDate(weeklyEntity.getCandleDataDate())
                .priceOpen(weeklyEntity.getPriceOpen())
                .priceHigh(weeklyEntity.getPriceHigh())
                .priceLow(weeklyEntity.getPriceLow())
                .priceClose(weeklyEntity.getPriceClose())
                .volume(weeklyEntity.getVolume())
                .build();
    }
}
