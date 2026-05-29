package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.DailyPrice;

public class OhlcvMapper {
    public static OhlcvDTO toDTO(DailyPrice dailyPrice) {
        return OhlcvDTO.builder()
                .priceDate(dailyPrice.getPriceDate())
                .priceOpen(dailyPrice.getPriceOpen())
                .priceHigh(dailyPrice.getPriceHigh())
                .priceLow(dailyPrice.getPriceLow())
                .priceClose(dailyPrice.getPriceClose())
                .volume(dailyPrice.getVolume())
                .build();
    }
}
