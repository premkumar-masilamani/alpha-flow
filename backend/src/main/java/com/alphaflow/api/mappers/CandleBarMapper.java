package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.CandleBarDTO;
import com.alphaflow.infrastructure.entities.CandleData;

public class CandleBarMapper {
    public static CandleBarDTO toDTO(CandleData candleEntity) {
        return CandleBarDTO.builder()
                .candleBarDate(candleEntity.getCandleBarDate())
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
