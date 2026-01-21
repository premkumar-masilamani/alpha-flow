package com.alphaflow.common.mappers;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.common.entities.MarketData;

public class MarketDataMapper {
    public static MarketDataDTO toDTO(MarketData marketData) {
        return new MarketDataDTO(
                marketData.getMarketDataDate(),
                marketData.getPriceOpen(),
                marketData.getPriceHigh(),
                marketData.getPriceLow(),
                marketData.getPriceClose(),
                marketData.getVolume(),
                marketData.getVwap(),
                marketData.getVolumeProfilePOC(),
                marketData.getVolumeProfileVAH(),
                marketData.getVolumeProfileVAL(),
                marketData.getBuyerVolumeShare(),
                marketData.getBuyerCapitalShare()
        );
    }
}
