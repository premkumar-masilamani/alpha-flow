package com.alphaflow.api.services;

import com.alphaflow.api.configs.ApiProperties;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailyPriceServiceTest {

    @Test
    void testGetDailyPriceByTickerNameSuccess() {
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        TickerRepository tickerRepo = mock(TickerRepository.class);
        ApiProperties apiProperties = mock(ApiProperties.class);

        when(tickerRepo.existsByTickerSymbolIgnoreCase("AAPL")).thenReturn(true);
        when(apiProperties.windowFor(Timeframe.DAILY)).thenReturn(180);

        DailyPrice dp1 = DailyPrice.builder()
                .priceDate(LocalDate.of(2026, 5, 29))
                .priceOpen(new BigDecimal("100.0000"))
                .priceHigh(new BigDecimal("105.0000"))
                .priceLow(new BigDecimal("99.0000"))
                .priceClose(new BigDecimal("102.0000"))
                .volume(1000L)
                .build();
        DailyPrice dp2 = DailyPrice.builder()
                .priceDate(LocalDate.of(2026, 5, 28))
                .priceOpen(new BigDecimal("98.0000"))
                .priceHigh(new BigDecimal("101.0000"))
                .priceLow(new BigDecimal("97.0000"))
                .priceClose(new BigDecimal("99.0000"))
                .volume(800L)
                .build();

        // Database return is descending/latest first usually or in any order, service sorts them ascending
        when(dailyRepo.findLatestByTickerName("AAPL", PageRequest.of(0, 180)))
                .thenReturn(List.of(dp1, dp2));

        DailyPriceService service = new DailyPriceService(dailyRepo, tickerRepo, apiProperties);
        List<OhlcvDTO> result = service.getDailyPriceByTickerName("AAPL");

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2026, 5, 28), result.get(0).priceDate()); // Sorted ascending by date
        assertEquals(LocalDate.of(2026, 5, 29), result.get(1).priceDate());
    }

    @Test
    void testGetDailyPriceByTickerNameNotFound() {
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        TickerRepository tickerRepo = mock(TickerRepository.class);
        ApiProperties apiProperties = mock(ApiProperties.class);

        when(tickerRepo.existsByTickerSymbolIgnoreCase("INVALID")).thenReturn(false);

        DailyPriceService service = new DailyPriceService(dailyRepo, tickerRepo, apiProperties);
        assertThrows(ResourceNotFoundException.class, () -> service.getDailyPriceByTickerName("INVALID"));
    }
}
