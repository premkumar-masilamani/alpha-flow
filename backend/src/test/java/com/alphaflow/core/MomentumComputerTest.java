package com.alphaflow.core;

import com.alphaflow.domain.enums.MarketDataMetricType;
import com.alphaflow.domain.enums.TransformationType;
import com.alphaflow.domain.enums.WindowPeriod;
import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
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

import static com.alphaflow.infrastructure.config.Constants.EPOCH_START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MomentumComputerTest {

    @Mock
    private MarketStateRepository marketStateRepository;

    @Mock
    private TickerRepository tickerRepository;

    private MomentumComputer momentumComputer;

    @BeforeEach
    void setUp() {
        momentumComputer = new MomentumComputer(marketStateRepository, tickerRepository);
    }

    @Test
    void compute_calculatesMomentumCorrectlyForNewData() {
        Ticker ticker = new Ticker();
        ticker.setTickerSymbol("BTCUSDT");

        LocalDate today = LocalDate.now();

        MarketState latestEma10 = new MarketState();
        latestEma10.setMarketStateDate(today);

        MarketState ema10 = new MarketState();
        ema10.setMarketStateDate(today);
        ema10.setValue(new BigDecimal("100.00"));

        MarketState ema20 = new MarketState();
        ema20.setMarketStateDate(today);
        ema20.setValue(new BigDecimal("90.00"));

        when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
        // No existing momentum
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, "CAP_MOM", "NONE", 0)).thenReturn(Optional.empty());

        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, "T_CAP", "EMA", 10)).thenReturn(Optional.of(latestEma10));

        when(marketStateRepository.findByTickerAndMetricAndMaTypeAndPeriodAndMarketStateDateGreaterThanEqualOrderByMarketStateDateAsc(
                ticker, "T_CAP", "EMA", 10, EPOCH_START)).thenReturn(List.of(ema10));

        when(marketStateRepository.findByTickerAndMetricAndMaTypeAndPeriodAndMarketStateDateGreaterThanEqualOrderByMarketStateDateAsc(
                ticker, "T_CAP", "EMA", 20, EPOCH_START)).thenReturn(List.of(ema20));

        momentumComputer.compute();

        ArgumentCaptor<List<MarketState>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketStateRepository).saveAll(captor.capture());

        MarketState saved = captor.getValue().get(0);
        assertEquals("CAP_MOM", saved.getMetric());
        assertEquals(new BigDecimal("10.00"), saved.getValue());
        assertEquals("NONE", saved.getMaType());
        assertEquals(0, saved.getPeriod());
    }

    @Test
    void compute_skipsIfUpToDate() {
        Ticker ticker = new Ticker();
        ticker.setTickerSymbol("BTCUSDT");

        LocalDate today = LocalDate.now();

        MarketState latestMom = new MarketState();
        latestMom.setMarketStateDate(today);

        MarketState latestEma10 = new MarketState();
        latestEma10.setMarketStateDate(today);

        when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, "CAP_MOM", "NONE", 0)).thenReturn(Optional.of(latestMom));

        when(marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                ticker, "T_CAP", "EMA", 10)).thenReturn(Optional.of(latestEma10));

        momentumComputer.compute();

        verify(marketStateRepository, never()).save(any());
    }
}
