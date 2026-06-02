package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.WeeklyPrice;

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

    public static OhlcvDTO toDTO(WeeklyPrice weeklyPrice) {
        return OhlcvDTO.builder()
            .priceDate(weeklyPrice.getPriceDate())
            .priceOpen(weeklyPrice.getPriceOpen())
            .priceHigh(weeklyPrice.getPriceHigh())
            .priceLow(weeklyPrice.getPriceLow())
            .priceClose(weeklyPrice.getPriceClose())
            .volume(weeklyPrice.getVolume())
            .build();
    }
}
