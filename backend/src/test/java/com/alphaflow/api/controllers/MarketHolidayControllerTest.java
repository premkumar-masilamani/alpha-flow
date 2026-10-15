package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.api.services.MarketHolidayService;
import com.alphaflow.persistence.enums.Country;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketHolidayControllerTest {

  @Test
  void testGetHolidays() {
    MarketHolidayService service = mock(MarketHolidayService.class);
    MarketHolidayDto dto =
        MarketHolidayDto.builder()
            .id(1L)
            .country(Country.US)
            .holidayDate(LocalDate.of(2026, 1, 1))
            .name("New Year's Day")
            .build();

    when(service.getHolidays()).thenReturn(List.of(dto));

    MarketHolidayController controller = new MarketHolidayController(service);
    List<MarketHolidayDto> result = controller.getHolidays();

    assertEquals(1, result.size());
    assertEquals("New Year's Day", result.getFirst().name());
    verify(service).getHolidays();
  }
}
