package com.alphaflow.core;

import com.alphaflow.domain.enums.MarketDataMetricType;
import com.alphaflow.domain.enums.TransformationType;
import com.alphaflow.domain.enums.WindowPeriod;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OBVComputerTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private MarketStateRepository marketStateRepository;

    @Mock
    private TickerRepository tickerRepository;

    private OBVComputer obvComputer;

    @BeforeEach
    void setUp() {
        obvComputer = new OBVComputer(marketDataRepository, marketStateRepository, tickerRepository);
    }

    @Test
    void compute_calculatesOBVCorrectlyFromScratch() {
        Ticker ticker = new Ticker();
        ticker.setTickerSymbol("BTCUSDT");

        LocalDate day0 = LocalDate.of(2023, 1, 1);
        LocalDate day1 = LocalDate.of(2023, 1, 2);
        LocalDate day2 = LocalDate.of(2023, 1, 3);
        LocalDate day3 = LocalDate.of(2023, 1, 4);

        MarketData md0 = createMarketData(ticker, day0, "100", "10");
        MarketData md1 = createMarketData(ticker, day1, "110", "20"); // Up
        MarketData md2 = createMarketData(ticker, day2, "105", "15"); // Down
        MarketData md3 = createMarketData(ticker, day3, "105", "30"); // Neutral

        when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(ticker, EPOCH))
                .thenReturn(List.of(md0, md1, md2, md3));

        // No existing OBV
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, "OBV", "NONE", 0)).thenReturn(Optional.empty());

        // For persist idempotency checks
        when(marketStateRepository.findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(any(), any(), any(), any(), anyInt()))
                .thenReturn(Optional.empty());

        obvComputer.compute();

        ArgumentCaptor<MarketState> captor = ArgumentCaptor.forClass(MarketState.class);
        verify(marketStateRepository, times(4)).save(captor.capture());

        List<MarketState> savedStates = captor.getAllValues();

        // Day 0: Initial OBV = Vol 0 = 10
        assertEquals(new BigDecimal("10"), savedStates.get(0).getValue());
        assertEquals(day0, savedStates.get(0).getMarketStateDate());

        // Day 1: 110 > 100 -> OBV = 10 + 20 = 30
        assertEquals(new BigDecimal("30"), savedStates.get(1).getValue());
        assertEquals(day1, savedStates.get(1).getMarketStateDate());

        // Day 2: 105 < 110 -> OBV = 30 - 15 = 15
        assertEquals(new BigDecimal("15"), savedStates.get(2).getValue());
        assertEquals(day2, savedStates.get(2).getMarketStateDate());

        // Day 3: 105 == 105 -> OBV = 15
        assertEquals(new BigDecimal("15"), savedStates.get(3).getValue());
        assertEquals(day3, savedStates.get(3).getMarketStateDate());
    }

    @Test
    void compute_resumesOBVCorrectly() {
        Ticker ticker = new Ticker();
        ticker.setTickerSymbol("BTCUSDT");

        LocalDate day0 = LocalDate.of(2023, 1, 1);
        LocalDate day1 = LocalDate.of(2023, 1, 2);

        MarketData md0 = createMarketData(ticker, day0, "100", "10");
        MarketData md1 = createMarketData(ticker, day1, "110", "20"); // Up

        when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(ticker, EPOCH))
                .thenReturn(List.of(md0, md1));

        // Existing OBV for Day 0
        MarketState existingObv = new MarketState();
        existingObv.setMarketStateDate(day0);
        existingObv.setValue(new BigDecimal("10"));
        existingObv.setMetric("OBV");
        existingObv.setMaType("NONE");
        existingObv.setPeriod(0);

        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, "OBV", "NONE", 0)).thenReturn(Optional.of(existingObv));

        when(marketStateRepository.findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(any(), any(), any(), any(), anyInt()))
                .thenReturn(Optional.empty());

        obvComputer.compute();

        ArgumentCaptor<MarketState> captor = ArgumentCaptor.forClass(MarketState.class);
        // Should only save for Day 1
        verify(marketStateRepository, times(1)).save(captor.capture());

        MarketState saved = captor.getValue();
        assertEquals(day1, saved.getMarketStateDate());
        assertEquals(new BigDecimal("30"), saved.getValue());
    }

    private MarketData createMarketData(Ticker ticker, LocalDate date, String close, String volume) {
        MarketData md = new MarketData();
        md.setTicker(ticker);
        md.setMarketDataDate(date);
        md.setPriceClose(new BigDecimal(close));
        md.setVolume(new BigDecimal(volume));
        return md;
    }
}
