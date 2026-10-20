package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.persistence.entities.MarketHoliday;

public final class MarketHolidayMapper {

  public static MarketHolidayDto toDto(MarketHoliday marketHoliday) {
    if (marketHoliday != null) {
      return MarketHolidayDto.builder()
          .id(marketHoliday.getMarketHolidayId())
          .country(marketHoliday.getCountry())
          .holidayDate(marketHoliday.getHolidayDate())
          .name(marketHoliday.getName())
          .build();
    } else {
      return null;
    }
  }
}
