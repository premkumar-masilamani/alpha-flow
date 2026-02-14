package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.CandleDataDTO;
import com.alphaflow.infrastructure.entities.CandleData;

public class CandleDataMapper {
    public static CandleDataDTO toDTO(CandleData candleEntity) {
        return CandleDataDTO.builder()
                .candleDataDate(candleEntity.getCandleDataDate())
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
