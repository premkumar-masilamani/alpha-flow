package com.alphaflow.infrastructure.persistence.mappers;

import com.alphaflow.api.dtos.MarketDataDTO;
import com.alphaflow.infrastructure.persistence.entities.MarketData;

public class MarketDataMapper {
    public static MarketDataDTO toDTO(MarketData marketDataEntity) {
        return new MarketDataDTO(
                marketDataEntity.getMarketDataDate(),
                marketDataEntity.getPriceOpen(),
                marketDataEntity.getPriceHigh(),
                marketDataEntity.getPriceLow(),
                marketDataEntity.getPriceClose(),
                marketDataEntity.getVolume(),
                marketDataEntity.getVwap(),
                marketDataEntity.getVolumeProfilePOC(),
                marketDataEntity.getVolumeProfileVAH(),
                marketDataEntity.getVolumeProfileVAL(),
                marketDataEntity.getBuyerVolumeShare(),
                marketDataEntity.getBuyerCapitalShare()
        );
    }
}
