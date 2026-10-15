package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.api.mappers.MarketHolidayMapper;
import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.MarketHolidayRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketHolidayServiceTest {

  private MarketHolidayRepository marketHolidayRepository;
  private MarketHolidayService marketHolidayService;

  @BeforeEach
  void setUp() {
    marketHolidayRepository = mock(MarketHolidayRepository.class);
    marketHolidayService = new MarketHolidayService(marketHolidayRepository);
  }

  @Test
  void testGetHolidaysReturnsAllRecords() {
    MarketHoliday holiday =
        MarketHoliday.builder()
            .marketHolidayId(1L)
            .country(Country.US)
            .holidayDate(LocalDate.of(2026, 1, 1))
            .name("New Year's Day")
            .build();

    when(marketHolidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(List.of(holiday));

    List<MarketHolidayDto> result = marketHolidayService.getHolidays();

    assertEquals(1, result.size());
    assertEquals(1L, result.getFirst().id());
    assertEquals(Country.US, result.getFirst().country());
    assertEquals(LocalDate.of(2026, 1, 1), result.getFirst().holidayDate());
    assertEquals("New Year's Day", result.getFirst().name());
    verify(marketHolidayRepository).findAllByOrderByHolidayDateAsc();
  }

  @Test
  void testMarketHolidayMapperNull() {
    assertNull(MarketHolidayMapper.toDto(null));
  }
}
