package com.alphaflow.engine.calculation;

import com.alphaflow.engine.enums.MarketDataMetricType;
import com.alphaflow.engine.enums.TransformationType;
import com.alphaflow.engine.enums.WindowPeriod;
import com.alphaflow.infrastructure.entities.MarketState;
import com.alphaflow.infrastructure.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
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

import static com.alphaflow.engine.enums.MarketDataMetricType.TOTAL_CAPITAL;
import static com.alphaflow.engine.enums.TransformationType.EMA;
import static com.alphaflow.engine.enums.WindowPeriod.TEN_DAYS;
import static com.alphaflow.engine.enums.WindowPeriod.TWENTY_DAYS;
import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;
import static java.time.LocalDate.EPOCH;

@Service
public class MarketStateDerivativeCalculator {

    private static final Logger log = LoggerFactory.getLogger(MarketStateDerivativeCalculator.class);

    private final MarketStateRepository marketStateRepository;
    private final TickerRepository tickerRepository;

    public MarketStateDerivativeCalculator(
            MarketStateRepository marketStateRepository,
            TickerRepository tickerRepository
    ) {
        this.marketStateRepository = marketStateRepository;
        this.tickerRepository = tickerRepository;
    }

    public void calculate() {
        log.info("Starting Capital Momentum Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Calculating Capital Momentum for {}", ticker.getTickerSymbol());

            Optional<MarketState> latestCapitalMomentum = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                    ticker, MarketDataMetricType.CAPITAL_MOMENTUM.code(), TransformationType.CAP_MOM.code(), WindowPeriod.ZERO_DAYS.days()
            );

            Optional<MarketState> latestTotalCapitalEma10 = marketStateRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
                    ticker, TOTAL_CAPITAL.code(), EMA.code(), TEN_DAYS.days()
            );

            if (latestTotalCapitalEma10.isEmpty()) {
                log.debug("No {} EMA 10 found for {}, skipping momentum.", TOTAL_CAPITAL.code(), ticker.getTickerSymbol());
                return;
            }

            LocalDate startDate = latestCapitalMomentum.map(m -> m.getMarketStateDate().plusDays(1)).orElse(EPOCH);

            if (startDate.isAfter(latestTotalCapitalEma10.get().getMarketStateDate())) {
                log.debug("Capital Momentum is up to date for {}", ticker.getTickerSymbol());
                return;
            }

            log.info("Computing Capital Momentum for {} from {} to {}",
                    ticker.getTickerSymbol(), startDate, latestTotalCapitalEma10.get().getMarketStateDate());

            List<MarketState> totalCapitalEma10Series = marketStateRepository.findByTickerAndMetricAndMaTypeAndPeriodAndMarketStateDateGreaterThanEqualOrderByMarketStateDateAsc(
                    ticker, TOTAL_CAPITAL.code(), EMA.code(), TEN_DAYS.days(), startDate
            );

            List<MarketState> totalCapitalEma20Series = marketStateRepository.findByTickerAndMetricAndMaTypeAndPeriodAndMarketStateDateGreaterThanEqualOrderByMarketStateDateAsc(
                    ticker, TOTAL_CAPITAL.code(), EMA.code(), TWENTY_DAYS.days(), startDate
            );

            Map<LocalDate, MarketState> totalCapitalEma20Map = totalCapitalEma20Series.stream()
                    .collect(Collectors.toMap(MarketState::getMarketStateDate, Function.identity()));

            List<MarketState> toSave = new ArrayList<>();
            for (MarketState ema10 : totalCapitalEma10Series) {
                MarketState ema20 = totalCapitalEma20Map.get(ema10.getMarketStateDate());
                if (ema20 != null) {
                    BigDecimal momentum = ema10.getValue().subtract(ema20.getValue(), DB_MATH_CONTEXT);
                    toSave.add(MarketState.builder()
                            .ticker(ticker)
                            .marketStateDate(ema10.getMarketStateDate())
                            .metric(MarketDataMetricType.CAPITAL_MOMENTUM.code())
                            .maType(TransformationType.CAP_MOM.code())
                            .period(WindowPeriod.ZERO_DAYS.days())
                            .value(momentum)
                            .build());
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
