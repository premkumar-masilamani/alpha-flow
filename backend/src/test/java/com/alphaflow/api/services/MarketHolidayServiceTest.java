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
  void testGetHolidaysWithCountryAndYear() {
    LocalDate startDate = LocalDate.of(2026, 1, 1);
    LocalDate endDate = LocalDate.of(2026, 12, 31);
    MarketHoliday holiday =
        MarketHoliday.builder()
            .marketHolidayId(1L)
            .country(Country.US)
            .holidayDate(LocalDate.of(2026, 1, 1))
            .name("New Year's Day")
            .build();

    when(marketHolidayRepository.findByCountryAndHolidayDateBetweenOrderByHolidayDateAsc(
            Country.US, startDate, endDate))
        .thenReturn(List.of(holiday));

    List<MarketHolidayDto> result = marketHolidayService.getHolidays(Country.US, 2026);

    assertEquals(1, result.size());
    assertEquals("New Year's Day", result.getFirst().name());
    verify(marketHolidayRepository)
        .findByCountryAndHolidayDateBetweenOrderByHolidayDateAsc(Country.US, startDate, endDate);
  }

  @Test
  void testGetHolidaysWithCountryOnly() {
    MarketHoliday holiday =
        MarketHoliday.builder()
            .marketHolidayId(2L)
            .country(Country.IN)
            .holidayDate(LocalDate.of(2026, 1, 26))
            .name("Republic Day")
            .build();

    when(marketHolidayRepository.findByCountryOrderByHolidayDateAsc(Country.IN))
        .thenReturn(List.of(holiday));

    List<MarketHolidayDto> result = marketHolidayService.getHolidays(Country.IN, null);

    assertEquals(1, result.size());
    assertEquals("Republic Day", result.getFirst().name());
    verify(marketHolidayRepository).findByCountryOrderByHolidayDateAsc(Country.IN);
  }

  @Test
  void testGetHolidaysWithYearOnly() {
    LocalDate startDate = LocalDate.of(2027, 1, 1);
    LocalDate endDate = LocalDate.of(2027, 12, 31);
    MarketHoliday holiday =
        MarketHoliday.builder()
            .marketHolidayId(3L)
            .country(Country.US)
            .holidayDate(LocalDate.of(2027, 1, 1))
            .name("New Year's Day")
            .build();

    when(marketHolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate))
        .thenReturn(List.of(holiday));

    List<MarketHolidayDto> result = marketHolidayService.getHolidays(null, 2027);

    assertEquals(1, result.size());
    assertEquals("New Year's Day", result.getFirst().name());
    verify(marketHolidayRepository)
        .findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate);
  }

  @Test
  void testGetHolidaysWithoutCountryAndWithoutYear() {
    MarketHoliday holiday =
        MarketHoliday.builder()
            .marketHolidayId(4L)
            .country(Country.US)
            .holidayDate(LocalDate.of(2026, 7, 3))
            .name("Independence Day (observed)")
            .build();

    when(marketHolidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(List.of(holiday));

    List<MarketHolidayDto> result = marketHolidayService.getHolidays(null, null);

    assertEquals(1, result.size());
    assertEquals("Independence Day (observed)", result.getFirst().name());
    verify(marketHolidayRepository).findAllByOrderByHolidayDateAsc();
  }

  @Test
  void testMarketHolidayMapperNull() {
    assertNull(MarketHolidayMapper.toDto(null));
  }
}
