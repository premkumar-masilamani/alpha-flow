package com.alphaflow.core;

import com.alphaflow.domain.enums.MarketDataMetricType;
import com.alphaflow.domain.enums.TransformationType;
import com.alphaflow.domain.enums.WindowPeriod;
import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alphaflow.infrastructure.config.Constants.DB_MATH_CONTEXT;
import static com.alphaflow.infrastructure.config.Constants.EPOCH_START;

@Service
public class MomentumComputer {

    private static final Logger log = LoggerFactory.getLogger(MomentumComputer.class);

    private final MarketStateRepository marketStateRepository;
    private final TickerRepository tickerRepository;

    public MomentumComputer(
            MarketStateRepository marketStateRepository,
            TickerRepository tickerRepository
    ) {
        this.marketStateRepository = marketStateRepository;
        this.tickerRepository = tickerRepository;
    }

    public void compute() {
        log.info("Starting Capital Momentum Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Processing Capital Momentum for {}", ticker.getTickerSymbol());

            Optional<MarketState> latestMom = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                    ticker, MarketDataMetricType.CAPITAL_MOMENTUM.code(), TransformationType.NONE.code(), WindowPeriod.NONE.days()
            );

            Optional<MarketState> latestEma10 = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                    ticker, MarketDataMetricType.TOTAL_CAPITAL.code(), TransformationType.EMA.code(), 10
            );

            if (latestEma10.isEmpty()) {
                log.debug("No T_CAP EMA 10 found for {}, skipping momentum.", ticker.getTickerSymbol());
                return;
            }

            LocalDate startDate = latestMom.map(m -> m.getMarketStateDate().plusDays(1)).orElse(EPOCH_START);

            if (startDate.isAfter(latestEma10.get().getMarketStateDate())) {
                log.debug("Capital Momentum is up to date for {}", ticker.getTickerSymbol());
                return;
            }

            log.info("Computing Capital Momentum for {} from {} to {}",
                    ticker.getTickerSymbol(), startDate, latestEma10.get().getMarketStateDate());

            List<MarketState> ema10Series = marketStateRepository.findByTickerAndMetricAndMaTypeAndPeriodAndMarketStateDateGreaterThanEqualOrderByMarketStateDateAsc(
                    ticker, MarketDataMetricType.TOTAL_CAPITAL.code(), TransformationType.EMA.code(), 10, startDate
            );

            List<MarketState> ema20Series = marketStateRepository.findByTickerAndMetricAndMaTypeAndPeriodAndMarketStateDateGreaterThanEqualOrderByMarketStateDateAsc(
                    ticker, MarketDataMetricType.TOTAL_CAPITAL.code(), TransformationType.EMA.code(), 20, startDate
            );

            Map<LocalDate, MarketState> ema20Map = ema20Series.stream()
                    .collect(Collectors.toMap(MarketState::getMarketStateDate, Function.identity()));

            List<MarketState> toSave = new ArrayList<>();
            for (MarketState ema10 : ema10Series) {
                MarketState ema20 = ema20Map.get(ema10.getMarketStateDate());
                if (ema20 != null) {
                    BigDecimal momentum = ema10.getValue().subtract(ema20.getValue(), DB_MATH_CONTEXT);

                    MarketState state = new MarketState();
                    state.setTicker(ticker);
                    state.setMarketStateDate(ema10.getMarketStateDate());
                    state.setMetric(MarketDataMetricType.CAPITAL_MOMENTUM.code());
                    state.setMaType(TransformationType.NONE.code());
                    state.setPeriod(WindowPeriod.NONE.days());
                    state.setValue(momentum);
                    toSave.add(state);
                }
            }
            if (!toSave.isEmpty()) {
                marketStateRepository.saveAll(toSave);
                log.info("Added {} new Capital Momentum records for {}", toSave.size(), ticker.getTickerSymbol());
            } else {
                log.debug("No new Capital Momentum records to add for {}", ticker.getTickerSymbol());
            }
        });

        log.info("Completed Capital Momentum Computation");
    }
}
