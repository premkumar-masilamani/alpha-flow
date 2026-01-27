package com.alphaflow.core;

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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.alphaflow.domain.enums.TransformationType;
import com.alphaflow.domain.enums.WindowPeriod;


import static com.alphaflow.domain.enums.MarketDataMetricType.OBV;
import static java.time.LocalDate.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OnBalanceVolumeComputerTest {

    @Mock
    private MarketDataRepository marketDataRepository;
    @Mock
    private MarketStateRepository marketStateRepository;
    @Mock
    private TickerRepository tickerRepository;

    @InjectMocks
    private OnBalanceVolumeComputer onBalanceVolumeComputer;

    @Captor
    private ArgumentCaptor<List<MarketState>> marketStateListCaptor;

    private Ticker ticker;

    @BeforeEach
    void setUp() {
        ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("TEST");
        ticker.setActive(true);
    }

    @Test
    void testCompute_initialCalculation() {
        // given
        when(tickerRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(ticker));
        List<MarketData> marketDataList = List.of(
            createMarketData(LocalDate.of(2023, 1, 1), "100", "10"),
            createMarketData(LocalDate.of(2023, 1, 2), "101", "12"), // price up
            createMarketData(LocalDate.of(2023, 1, 3), "100", "8"),  // price down
            createMarketData(LocalDate.of(2023, 1, 4), "100", "9")   // price same
        );
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(ticker, EPOCH))
            .thenReturn(marketDataList);
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
            ticker, OBV.code(), TransformationType.NONE.code(), WindowPeriod.NONE.days()
        )).thenReturn(Optional.empty());

        // when
        onBalanceVolumeComputer.compute();

        // then
        verify(marketStateRepository).saveAll(marketStateListCaptor.capture());
        List<MarketState> savedStates = marketStateListCaptor.getValue();

        assertEquals(4, savedStates.size());
        // Day 1: OBV is 0
        assertEquals(new BigDecimal("0"), savedStates.get(0).getValue());
        // Day 2: Price up, OBV = 0 + 12 = 12
        assertEquals(new BigDecimal("12"), savedStates.get(1).getValue());
        // Day 3: Price down, OBV = 12 - 8 = 4
        assertEquals(new BigDecimal("4"), savedStates.get(2).getValue());
        // Day 4: Price same, OBV = 4
        assertEquals(new BigDecimal("4"), savedStates.get(3).getValue());
    }

    @Test
    void testCompute_incrementalCalculation() {
        // given
        when(tickerRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(ticker));
        MarketState latestObv = new MarketState();
        latestObv.setMarketStateDate(LocalDate.of(2023, 1, 2));
        latestObv.setValue(new BigDecimal("12"));
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
            ticker, OBV.code(), TransformationType.NONE.code(), WindowPeriod.NONE.days()
        )).thenReturn(Optional.of(latestObv));

        List<MarketData> marketDataList = List.of(
            createMarketData(LocalDate.of(2023, 1, 1), "100", "10"),
            createMarketData(LocalDate.of(2023, 1, 2), "101", "12"),
            createMarketData(LocalDate.of(2023, 1, 3), "100", "8"),
            createMarketData(LocalDate.of(2023, 1, 4), "101", "9")
        );
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(ticker, EPOCH))
            .thenReturn(marketDataList);

        // when
        onBalanceVolumeComputer.compute();

        // then
        verify(marketStateRepository).saveAll(marketStateListCaptor.capture());
        List<MarketState> savedStates = marketStateListCaptor.getValue();
        assertEquals(2, savedStates.size());
        // Day 3: Price down, OBV = 12 - 8 = 4
        assertEquals(new BigDecimal("4"), savedStates.get(0).getValue());
        // Day 4: Price up, OBV = 4 + 9 = 13
        assertEquals(new BigDecimal("13"), savedStates.get(1).getValue());
    }

    @Test
    void testCompute_noMarketData() {
        // given
        when(tickerRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(ticker));
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(ticker, EPOCH))
            .thenReturn(Collections.emptyList());

        // when
        onBalanceVolumeComputer.compute();

        // then
        verify(marketStateRepository, never()).saveAll(any());
    }

    @Test
    void testCompute_upToDate() {
        // given
        when(tickerRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(ticker));
        MarketState latestObv = new MarketState();
        latestObv.setMarketStateDate(LocalDate.of(2023, 1, 4));
        latestObv.setValue(new BigDecimal("13"));
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
            ticker, OBV.code(), TransformationType.NONE.code(), WindowPeriod.NONE.days()
        )).thenReturn(Optional.of(latestObv));

        List<MarketData> marketDataList = List.of(
            createMarketData(LocalDate.of(2023, 1, 3), "100", "8"),
            createMarketData(LocalDate.of(2023, 1, 4), "101", "9")
        );
        when(marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(ticker, EPOCH))
            .thenReturn(marketDataList);

        // when
        onBalanceVolumeComputer.compute();

        // then
        verify(marketStateRepository, never()).saveAll(any());
    }

    private MarketData createMarketData(LocalDate date, String closePrice, String volume) {
        MarketData md = new MarketData();
        md.setTicker(ticker);
        md.setMarketDataDate(date);
        md.setPriceClose(new BigDecimal(closePrice));
        md.setVolume(new BigDecimal(volume));
        return md;
    }
}
