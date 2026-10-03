package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.Country;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class OhlcvMapper {

  public static OhlcvDto toDto(DailyPrice dailyPrice) {
    Country country = dailyPrice.getTicker() != null ? dailyPrice.getTicker().getCountry() : null;
    return OhlcvDto.builder()
        .priceDate(toOffsetDateTime(dailyPrice.getPriceDate(), country))
        .priceOpen(dailyPrice.getPriceOpen())
        .priceHigh(dailyPrice.getPriceHigh())
        .priceLow(dailyPrice.getPriceLow())
        .priceClose(dailyPrice.getPriceClose())
        .volume(dailyPrice.getVolume())
        .build();
  }

  public static OhlcvDto toDto(WeeklyPrice weeklyPrice) {
    Country country = weeklyPrice.getTicker() != null ? weeklyPrice.getTicker().getCountry() : null;
    return OhlcvDto.builder()
        .priceDate(toOffsetDateTime(weeklyPrice.getPriceDate(), country))
        .priceOpen(weeklyPrice.getPriceOpen())
        .priceHigh(weeklyPrice.getPriceHigh())
        .priceLow(weeklyPrice.getPriceLow())
        .priceClose(weeklyPrice.getPriceClose())
        .volume(weeklyPrice.getVolume())
        .build();
  }

  public static OhlcvDto toDto(IntradayPrice intradayPrice) {
    return OhlcvDto.builder()
        .priceDate(intradayPrice.getPriceTime())
        .priceOpen(intradayPrice.getPriceOpen())
        .priceHigh(intradayPrice.getPriceHigh())
        .priceLow(intradayPrice.getPriceLow())
        .priceClose(intradayPrice.getPriceClose())
        .volume(intradayPrice.getVolume())
        .build();
  }

  private static OffsetDateTime toOffsetDateTime(LocalDate localDate, Country country) {
    if (localDate == null) {
      return null;
    }
    if (country == Country.IN) {
      return localDate.atStartOfDay(MarketConstants.IST_ZONE).toOffsetDateTime();
    }
    if (country == Country.US) {
      return localDate.atStartOfDay(MarketConstants.EST_ZONE).toOffsetDateTime();
    }
    return localDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
  }
}
