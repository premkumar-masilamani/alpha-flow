package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.WeeklyPrice;

public class OhlcvMapper {

  public static OhlcvDto toDto(DailyPrice dailyPrice) {
    return OhlcvDto.builder()
        .priceDate(dailyPrice.getPriceDate())
        .priceOpen(dailyPrice.getPriceOpen())
        .priceHigh(dailyPrice.getPriceHigh())
        .priceLow(dailyPrice.getPriceLow())
        .priceClose(dailyPrice.getPriceClose())
        .volume(dailyPrice.getVolume())
        .build();
  }

  public static OhlcvDto toDto(WeeklyPrice weeklyPrice) {
    return OhlcvDto.builder()
        .priceDate(weeklyPrice.getPriceDate())
        .priceOpen(weeklyPrice.getPriceOpen())
        .priceHigh(weeklyPrice.getPriceHigh())
        .priceLow(weeklyPrice.getPriceLow())
        .priceClose(weeklyPrice.getPriceClose())
        .volume(weeklyPrice.getVolume())
        .build();
  }
}
