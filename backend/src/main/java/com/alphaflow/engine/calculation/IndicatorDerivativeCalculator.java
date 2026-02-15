package com.alphaflow.engine.calculation;

import com.alphaflow.engine.enums.CandleDataMetricType;
import com.alphaflow.engine.enums.TransformationType;
import com.alphaflow.engine.enums.WindowPeriod;
import com.alphaflow.infrastructure.entities.Indicator;
import com.alphaflow.infrastructure.repositories.IndicatorRepository;
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

import static com.alphaflow.engine.enums.CandleDataMetricType.TOTAL_CAPITAL;
import static com.alphaflow.engine.enums.TransformationType.EMA;
import static com.alphaflow.engine.enums.WindowPeriod.TEN_DAYS;
import static com.alphaflow.engine.enums.WindowPeriod.TWENTY_DAYS;
import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;
import static java.time.LocalDate.EPOCH;

@Service
public class IndicatorDerivativeCalculator {

    private static final Logger log = LoggerFactory.getLogger(IndicatorDerivativeCalculator.class);

    private final IndicatorRepository indicatorRepository;
    private final TickerRepository tickerRepository;

    public IndicatorDerivativeCalculator(
            IndicatorRepository indicatorRepository,
            TickerRepository tickerRepository
    ) {
        this.indicatorRepository = indicatorRepository;
        this.tickerRepository = tickerRepository;
    }

    public void calculate() {
        log.info("Starting Capital Momentum Computation");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            if (!CandleDataMetricType.CAPITAL_MOMENTUM.isEligibleFor(ticker.getSource()) ||
                    CandleDataMetricType.CAPITAL_MOMENTUM.transformSpecs().isEmpty()) {
                return;
            }
            log.info("Calculating Capital Momentum for {}", ticker.getTickerSymbol());

            Optional<Indicator> latestCapitalMomentum = indicatorRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByIndicatorDateDesc(
                    ticker, CandleDataMetricType.CAPITAL_MOMENTUM.code(), TransformationType.CAP_MOM.code(), WindowPeriod.ZERO_DAYS.days()
            );

            Optional<Indicator> latestTotalCapitalEma10 = indicatorRepository.findTopByTickerAndMetricAndMaTypeAndPeriodOrderByIndicatorDateDesc(
                    ticker, TOTAL_CAPITAL.code(), EMA.code(), TEN_DAYS.days()
            );

            if (latestTotalCapitalEma10.isEmpty()) {
                log.debug("No {} EMA 10 found for {}, skipping momentum.", TOTAL_CAPITAL.code(), ticker.getTickerSymbol());
                return;
            }

            LocalDate startDate = latestCapitalMomentum.map(m -> m.getIndicatorDate().plusDays(1)).orElse(EPOCH);

            if (startDate.isAfter(latestTotalCapitalEma10.get().getIndicatorDate())) {
                log.debug("Capital Momentum is up to date for {}", ticker.getTickerSymbol());
                return;
            }

            log.info("Computing Capital Momentum for {} from {} to {}",
                    ticker.getTickerSymbol(), startDate, latestTotalCapitalEma10.get().getIndicatorDate());

            List<Indicator> totalCapitalEma10Series = indicatorRepository.findByTickerAndMetricAndMaTypeAndPeriodAndIndicatorDateGreaterThanEqualOrderByIndicatorDateAsc(
                    ticker, TOTAL_CAPITAL.code(), EMA.code(), TEN_DAYS.days(), startDate
            );

            List<Indicator> totalCapitalEma20Series = indicatorRepository.findByTickerAndMetricAndMaTypeAndPeriodAndIndicatorDateGreaterThanEqualOrderByIndicatorDateAsc(
                    ticker, TOTAL_CAPITAL.code(), EMA.code(), TWENTY_DAYS.days(), startDate
            );

            Map<LocalDate, Indicator> totalCapitalEma20Map = totalCapitalEma20Series.stream()
                    .collect(Collectors.toMap(Indicator::getIndicatorDate, Function.identity()));

            List<Indicator> toSave = new ArrayList<>();
            for (Indicator ema10 : totalCapitalEma10Series) {
                Indicator ema20 = totalCapitalEma20Map.get(ema10.getIndicatorDate());
                if (ema20 != null) {
                    BigDecimal momentum = ema10.getValue().subtract(ema20.getValue(), DB_MATH_CONTEXT);

                    Indicator indicator = indicatorRepository.findByTickerAndIndicatorDateAndMetricAndMaTypeAndPeriod(
                                    ticker,
                                    ema10.getIndicatorDate(),
                                    CandleDataMetricType.CAPITAL_MOMENTUM.code(),
                                    TransformationType.CAP_MOM.code(),
                                    WindowPeriod.ZERO_DAYS.days())
                            .orElseGet(Indicator::new);

                    indicator.setTicker(ticker);
                    indicator.setIndicatorDate(ema10.getIndicatorDate());
                    indicator.setMetric(CandleDataMetricType.CAPITAL_MOMENTUM.code());
                    indicator.setMaType(TransformationType.CAP_MOM.code());
                    indicator.setPeriod(WindowPeriod.ZERO_DAYS.days());
                    indicator.setValue(momentum);

                    toSave.add(indicator);
                }
            }
            if (!toSave.isEmpty()) {
                indicatorRepository.saveAll(toSave);
                log.info("Added {} new Capital Momentum records for {}", toSave.size(), ticker.getTickerSymbol());
            } else {
                log.debug("No new Capital Momentum records to add for {}", ticker.getTickerSymbol());
            }
        });

        log.info("Completed Capital Momentum Computation");
    }
}
