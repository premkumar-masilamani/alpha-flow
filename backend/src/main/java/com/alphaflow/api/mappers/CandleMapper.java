package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.CandleDTO;
import com.alphaflow.infrastructure.entities.Candle;

public class CandleMapper {
    public static CandleDTO toDTO(Candle candleEntity) {
        return CandleDTO.builder()
                .candleDate(candleEntity.getCandleDate())
                .priceOpen(candleEntity.getPriceOpen())
                .priceHigh(candleEntity.getPriceHigh())
                .priceLow(candleEntity.getPriceLow())
                .priceClose(candleEntity.getPriceClose())
                .volume(candleEntity.getVolume())
                .vwap(candleEntity.getVwap())
                .capitalPOC(candleEntity.getCapitalPOC())
                .capitalVAH(candleEntity.getCapitalVAH())
                .capitalVAL(candleEntity.getCapitalVAL())
                .buyerCapital(candleEntity.getBuyerCapital())
                .totalCapital(candleEntity.getTotalCapital()).build();
    }
}
