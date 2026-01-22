package com.alphaflow.core;

import com.alphaflow.domain.model.MarketStateMetricType;
import com.alphaflow.domain.model.MovingAveragePeriod;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MarketStateComputerTest {

    @Mock private MarketDataRepository marketDataRepository;
    @Mock private MarketStateRepository marketStateRepository;
    @Mock private TickerRepository tickerRepository;

    private MarketStateComputer marketStateComputer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        marketStateComputer = new MarketStateComputer(marketDataRepository, marketStateRepository, tickerRepository);
    }

    @Test
    void testCompute_NoNewData_ShouldNotCompute() {
        Ticker ticker = new Ticker();
        ticker.setTickerSymbol("BTCUSDT");
        ticker.setActive(true);

        LocalDate lastDate = LocalDate.now();
        MarketData marketData = new MarketData();
        marketData.setTicker(ticker);
        marketData.setMarketDataDate(lastDate);
        marketData.setVwap(new BigDecimal("100"));

        when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(any(), any()))
                .thenReturn(List.of(marketData));

        // Mock that we already have MarketState for the last date
        MarketState existingState = new MarketState();
        existingState.setMarketStateDate(lastDate);
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(any(), any(), any(), anyInt()))
                .thenReturn(Optional.of(existingState));

        // Stub findByTicker... to return the same existing state if called (to avoid NPE if persist is called)
        when(marketStateRepository.findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(any(), any(), any(), any(), anyInt()))
                .thenReturn(Optional.of(existingState));

        marketStateComputer.compute();

        // Verify that save was never called because it's already up to date
        verify(marketStateRepository, never()).save(any());
    }

    @Test
    void testCompute_LastDateMismatch_ShouldRecomputeAll() {
        Ticker ticker = new Ticker();
        ticker.setTickerSymbol("BTCUSDT");
        ticker.setActive(true);

        LocalDate date1 = LocalDate.of(2023, 10, 1);
        MarketData md1 = new MarketData();
        md1.setTicker(ticker);
        md1.setMarketDataDate(date1);
        md1.setVwap(new BigDecimal("100"));

        when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(any(), any()))
                .thenReturn(List.of(md1));

        // Mock that we have MarketState for a DIFFERENT date (e.g., in the future or just not in MD)
        MarketState existingState = new MarketState();
        existingState.setMarketStateDate(LocalDate.of(2023, 10, 2));
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(any(), any(), any(), anyInt()))
                .thenReturn(Optional.of(existingState));

        // Stub findBy...
        when(marketStateRepository.findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(any(), any(), any(), any(), anyInt()))
                .thenReturn(Optional.empty());

        marketStateComputer.compute();

        // Since we only have 1 data point and period is >= 7, it shouldn't actually save anything for SMA/EMA
        // But it should have tried to find the index and failed.
        verify(marketStateRepository, never()).save(any());
    }

    @Test
    void testCompute_SufficientDataForOneNewRecord_ShouldCompute() {
        Ticker ticker = new Ticker();
        ticker.setTickerSymbol("BTCUSDT");
        ticker.setActive(true);

        // Need at least period data. Let's use period = 7 (ONE_WEEK)
        // We'll provide 8 days of data, and mock that 7 days are already computed.
        int period = 7;
        List<MarketData> allSeries = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            MarketData md = new MarketData();
            md.setTicker(ticker);
            md.setMarketDataDate(LocalDate.of(2023, 10, 1).plusDays(i));
            md.setVwap(new BigDecimal(100 + i));
            md.setPriceOpen(new BigDecimal(100 + i));
            md.setPriceHigh(new BigDecimal(100 + i));
            md.setPriceLow(new BigDecimal(100 + i));
            md.setPriceClose(new BigDecimal(100 + i));
            md.setVolume(new BigDecimal(1000));
            md.setVolumeProfilePOC(new BigDecimal(100 + i));
            md.setVolumeProfileVAH(new BigDecimal(105 + i));
            md.setVolumeProfileVAL(new BigDecimal(95 + i));
            md.setBuyerVolumeShare(new BigDecimal("0.5"));
            md.setBuyerCapitalShare(new BigDecimal("0.5"));
            allSeries.add(md);
        }

        when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(any(), any()))
                .thenReturn(allSeries);

        // Mock that we have MarketState for the 7th day (index 6)
        LocalDate date7 = allSeries.get(6).getMarketDataDate();
        MarketState existingState = new MarketState();
        existingState.setMarketStateDate(date7);
        existingState.setValue(new BigDecimal("100"));

        // Return existing state only for period 7. Others might return empty and start fresh, which is fine.
        // To simplify, let's just mock for all periods.
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(any(), any(), any(), anyInt()))
                .thenReturn(Optional.of(existingState));

        // Mock findBy... to return empty for the 8th day, and existing for the 7th
        LocalDate date8 = allSeries.get(7).getMarketDataDate();
        when(marketStateRepository.findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(any(), eq(date8), any(), any(), anyInt()))
                .thenReturn(Optional.empty());
        when(marketStateRepository.findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(any(), eq(date7), any(), any(), anyInt()))
                .thenReturn(Optional.of(existingState));

        marketStateComputer.compute();

        // Verify save was called for date8
        ArgumentCaptor<MarketState> captor = ArgumentCaptor.forClass(MarketState.class);
        verify(marketStateRepository, atLeastOnce()).save(captor.capture());

        boolean foundDate8 = captor.getAllValues().stream()
                .anyMatch(ms -> ms.getMarketStateDate().equals(date8));
        assertEquals(true, foundDate8, "Should have saved MarketState for date8");

        // Ensure date7 was NOT saved again (redundant)
        boolean foundDate7 = captor.getAllValues().stream()
                .anyMatch(ms -> ms.getMarketStateDate().equals(date7));
        assertEquals(false, foundDate7, "Should NOT have saved MarketState for date7 (already exists)");
    }
}
