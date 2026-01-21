package com.prem.ta.mappers;

import com.prem.ta.dtos.MarketDataDTO;
import com.prem.ta.entities.MarketData;

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
                marketData.getVwapPocSpread(),
                marketData.getBuyerVolumeShare(),
                marketData.getBuyerCapitalShare()
        );
    }
}
