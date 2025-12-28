package com.prem.ta.mappers;

import com.prem.ta.dtos.TradeDataDTO;
import com.prem.ta.entities.TradeData;

public class TradeDataMapper {

    public static TradeDataDTO toDTO(TradeData tradeData) {
        return new TradeDataDTO(
                tradeData.getTradeDate(),
                tradeData.getPriceOpen(),
                tradeData.getPriceHigh(),
                tradeData.getPriceLow(),
                tradeData.getPriceClose(),
                tradeData.getVolume(),
                tradeData.getVolumeWeightedAveragePrice(),
                tradeData.getVolumeProfilePointOfControl(),
                tradeData.getVolumeProfileValueAreaHigh(),
                tradeData.getVolumeProfileValueAreaLow(),
                tradeData.getBuyerVolumeShare(),
                tradeData.getBuyerCapitalShare()
        );
    }
}
