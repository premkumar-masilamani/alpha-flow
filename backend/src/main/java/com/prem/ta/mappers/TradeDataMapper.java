package com.prem.ta.mappers;

import com.prem.ta.dtos.TradeDataDto;
import com.prem.ta.entities.TradeData;

public class TradeDataMapper {

    public static TradeDataDto toDto(TradeData tradeData) {
        return new TradeDataDto(
                tradeData.getTicker().getSymbol(),
                tradeData.getTradeDate(),
                tradeData.getPriceOpen(),
                tradeData.getPriceHigh(),
                tradeData.getPriceLow(),
                tradeData.getPriceClose(),
                tradeData.getVolume(),
                tradeData.getVwap(),
                tradeData.getBuyerVolumeRatio(),
                tradeData.getBuyerCapitalRatio()
        );
    }
}
