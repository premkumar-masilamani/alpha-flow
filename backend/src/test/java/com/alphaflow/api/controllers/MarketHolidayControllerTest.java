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
  void testGetHolidaysWithFilters() {
    MarketHolidayService service = mock(MarketHolidayService.class);
    MarketHolidayDto dto =
        MarketHolidayDto.builder()
            .id(1L)
            .country(Country.US)
            .holidayDate(LocalDate.of(2026, 1, 1))
            .name("New Year's Day")
            .build();

    when(service.getHolidays(Country.US, 2026)).thenReturn(List.of(dto));

    MarketHolidayController controller = new MarketHolidayController(service);
    List<MarketHolidayDto> result = controller.getHolidays(Country.US, 2026);

    assertEquals(1, result.size());
    assertEquals("New Year's Day", result.getFirst().name());
    verify(service).getHolidays(Country.US, 2026);
  }

  @Test
  void testGetHolidaysWithoutFilters() {
    MarketHolidayService service = mock(MarketHolidayService.class);
    MarketHolidayDto dto =
        MarketHolidayDto.builder()
            .id(2L)
            .country(Country.IN)
            .holidayDate(LocalDate.of(2026, 1, 26))
            .name("Republic Day")
            .build();

    when(service.getHolidays(null, null)).thenReturn(List.of(dto));

    MarketHolidayController controller = new MarketHolidayController(service);
    List<MarketHolidayDto> result = controller.getHolidays(null, null);

    assertEquals(1, result.size());
    assertEquals("Republic Day", result.getFirst().name());
    verify(service).getHolidays(null, null);
  }
}
