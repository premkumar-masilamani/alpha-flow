package com.alphaflow.infrastructure.persistence.mappers;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.infrastructure.persistence.entities.MarketData;

public class MarketDataMapper {
    public static MarketDataDTO toDTO(MarketData marketDataEntity) {
        return MarketDataDTO.builder()
                .marketDataDate(marketDataEntity.getMarketDataDate())
                .priceOpen(marketDataEntity.getPriceOpen())
                .priceHigh(marketDataEntity.getPriceHigh())
                .priceLow(marketDataEntity.getPriceLow())
                .priceClose(marketDataEntity.getPriceClose())
                .volume(marketDataEntity.getVolume())
                .vwap(marketDataEntity.getVwap())
                .capitalPOC(marketDataEntity.getCapitalPOC())
                .capitalVAH(marketDataEntity.getCapitalVAH())
                .capitalVAL(marketDataEntity.getCapitalVAL())
                .buyerCapital(marketDataEntity.getBuyerCapital())
                .totalCapital(marketDataEntity.getTotalCapital()).build();
    }
}
