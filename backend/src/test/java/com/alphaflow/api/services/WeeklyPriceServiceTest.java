package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.configs.ApiProperties;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class WeeklyPriceServiceTest {

  @Test
  void testGetWeeklyPriceByTickerNameSuccess() {
    WeeklyPriceRepository weeklyRepo = mock(WeeklyPriceRepository.class);
    TickerRepository tickerRepo = mock(TickerRepository.class);
    ApiProperties apiProperties = mock(ApiProperties.class);

    when(tickerRepo.existsByTickerSymbolIgnoreCase("AAPL")).thenReturn(true);
    when(apiProperties.windowFor(Timeframe.WEEKLY)).thenReturn(180);

    WeeklyPrice wp1 =
        WeeklyPrice.builder()
            .priceDate(LocalDate.of(2026, 5, 29))
            .priceOpen(new BigDecimal("100.0000"))
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("99.0000"))
            .priceClose(new BigDecimal("102.0000"))
            .volume(1000L)
            .build();
    WeeklyPrice wp2 =
        WeeklyPrice.builder()
            .priceDate(LocalDate.of(2026, 5, 22))
            .priceOpen(new BigDecimal("98.0000"))
            .priceHigh(new BigDecimal("101.0000"))
            .priceLow(new BigDecimal("97.0000"))
            .priceClose(new BigDecimal("99.0000"))
            .volume(800L)
            .build();

    when(weeklyRepo.findLatestByTickerName("AAPL", PageRequest.of(0, 180)))
        .thenReturn(List.of(wp1, wp2));

    WeeklyPriceService service = new WeeklyPriceService(weeklyRepo, tickerRepo, apiProperties);
    List<OhlcvDTO> result = service.getWeeklyPriceByTickerName("AAPL");

    assertEquals(2, result.size());
    assertEquals(LocalDate.of(2026, 5, 22), result.get(0).priceDate()); // Sorted ascending by date
    assertEquals(LocalDate.of(2026, 5, 29), result.get(1).priceDate());
  }

  @Test
  void testGetWeeklyPriceByTickerNameNotFound() {
    WeeklyPriceRepository weeklyRepo = mock(WeeklyPriceRepository.class);
    TickerRepository tickerRepo = mock(TickerRepository.class);
    ApiProperties apiProperties = mock(ApiProperties.class);

    when(tickerRepo.existsByTickerSymbolIgnoreCase("INVALID")).thenReturn(false);

    WeeklyPriceService service = new WeeklyPriceService(weeklyRepo, tickerRepo, apiProperties);
    assertThrows(
        ResourceNotFoundException.class, () -> service.getWeeklyPriceByTickerName("INVALID"));
  }

  @Test
  void testGetWeeklyPriceByTickerNameWithCustomPageAndSizeSuccess() {
    WeeklyPriceRepository weeklyRepo = mock(WeeklyPriceRepository.class);
    TickerRepository tickerRepo = mock(TickerRepository.class);
    ApiProperties apiProperties = mock(ApiProperties.class);

    when(tickerRepo.existsByTickerSymbolIgnoreCase("AAPL")).thenReturn(true);

    WeeklyPrice wp =
        WeeklyPrice.builder()
            .priceDate(LocalDate.of(2026, 5, 29))
            .priceOpen(new BigDecimal("100.0000"))
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("99.0000"))
            .priceClose(new BigDecimal("102.0000"))
            .volume(1000L)
            .build();

    when(weeklyRepo.findLatestByTickerName("AAPL", PageRequest.of(1, 10))).thenReturn(List.of(wp));

    WeeklyPriceService service = new WeeklyPriceService(weeklyRepo, tickerRepo, apiProperties);
    List<OhlcvDTO> result = service.getWeeklyPriceByTickerName("AAPL", 1, 10);

    assertEquals(1, result.size());
    assertEquals(LocalDate.of(2026, 5, 29), result.get(0).priceDate());
  }
}
