package com.alphaflow.engine.strategies;

import com.alphaflow.api.dtos.ASTAResponseDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.strategies.evaluators.ASTAEvaluationContext;
import com.alphaflow.engine.strategies.evaluators.CalculatedSignal;
import com.alphaflow.engine.strategies.evaluators.DailyEmaEvaluator;
import com.alphaflow.engine.strategies.evaluators.DailyRsiEvaluator;
import com.alphaflow.engine.strategies.evaluators.DailyStochasticEvaluator;
import com.alphaflow.engine.strategies.evaluators.DailyVolumeEvaluator;
import com.alphaflow.engine.strategies.evaluators.WeeklyMacdEvaluator;
import com.alphaflow.persistence.entities.ASTAResults;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.enums.TradeAction;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.ASTAResultsRepository;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class ASTAStrategy {

  private static final int HISTORY_WINDOW = 5;

  private final TickerRepository tickerRepository;
  private final ASTAResultsRepository astaResultsRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;
  private final IndicatorConfig indicatorConfig;

  private final WeeklyMacdEvaluator weeklyMacdEvaluator;
  private final DailyStochasticEvaluator dailyStochasticEvaluator;
  private final DailyRsiEvaluator dailyRsiEvaluator;
  private final DailyVolumeEvaluator dailyVolumeEvaluator;
  private final DailyEmaEvaluator dailyEmaEvaluator;

  @Autowired @Lazy private ASTAStrategy astaStrategy;

  public ASTAStrategy(
      TickerRepository tickerRepository,
      ASTAResultsRepository astaResultsRepository,
      DailyPriceRepository dailyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository,
      IndicatorConfig indicatorConfig,
      WeeklyMacdEvaluator weeklyMacdEvaluator,
      DailyStochasticEvaluator dailyStochasticEvaluator,
      DailyRsiEvaluator dailyRsiEvaluator,
      DailyVolumeEvaluator dailyVolumeEvaluator,
      DailyEmaEvaluator dailyEmaEvaluator) {
    this.tickerRepository = tickerRepository;
    this.astaResultsRepository = astaResultsRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
    this.indicatorConfig = indicatorConfig;
    this.weeklyMacdEvaluator = weeklyMacdEvaluator;
    this.dailyStochasticEvaluator = dailyStochasticEvaluator;
    this.dailyRsiEvaluator = dailyRsiEvaluator;
    this.dailyVolumeEvaluator = dailyVolumeEvaluator;
    this.dailyEmaEvaluator = dailyEmaEvaluator;
  }

  /** Scheduled update step. Run this after indicator calculation. */
  public void computeTechnicalAnalysis() {
    log.info("Starting Technical Analysis computation for all active tickers...");
    List<Ticker> activeTickers = tickerRepository.findByIsActiveTrue();

    ASTAStrategy proxy = (astaStrategy != null) ? astaStrategy : this;
    for (Ticker ticker : activeTickers) {
      try {
        Optional<LocalDate> earliestMissingDateOpt =
            dailyPriceRepository.findEarliestDateMissingAnalysis(ticker);
        if (earliestMissingDateOpt.isPresent()) {
          LocalDate earliestMissingDate = earliestMissingDateOpt.get();
          log.info(
              "Ticker {} earliest missing analysis date: {}",
              ticker.getTickerSymbol(),
              earliestMissingDate);
          proxy.processTicker(ticker, earliestMissingDate);
        } else {
          log.info("Ticker {} has no missing analysis dates.", ticker.getTickerSymbol());
        }
      } catch (Exception e) {
        log.error(
            "Failed to compute technical analysis for ticker: {}", ticker.getTickerSymbol(), e);
      }
    }
    log.info("Technical Analysis computation finished.");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<ASTAResults> processTicker(Ticker ticker, LocalDate earliestMissingDate) {
    LocalDate startDate = earliestMissingDate.minusDays(HISTORY_WINDOW);

    // 1. Fetch Daily Prices
    List<DailyPrice> allDailyPrices =
        dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            ticker, startDate);
    if (allDailyPrices.isEmpty()) {
      return List.of();
    }

    // Filter to get the list of dates we actually want to compute analysis results for (dates >=
    // earliestMissingDate)
    List<LocalDate> missingDates =
        allDailyPrices.stream()
            .map(DailyPrice::getPriceDate)
            .filter(d -> !d.isBefore(earliestMissingDate))
            .toList();

    if (missingDates.isEmpty()) {
      return List.of();
    }

    // 2. Fetch Daily Indicators
    List<IndicatorDefinition> dailyDefs = indicatorConfig.forTimeframe(Timeframe.DAILY);
    List<Long> dailyIds = dailyDefs.stream().map(IndicatorDefinition::getIndicatorId).toList();
    List<DailyIndicator> allDailyIndicators =
        dailyIndicatorRepository.findSeriesFrom(ticker, dailyIds, startDate);

    // 3. Fetch Weekly Indicators
    List<IndicatorDefinition> weeklyDefs = indicatorConfig.forTimeframe(Timeframe.WEEKLY);
    List<Long> weeklyIds = weeklyDefs.stream().map(IndicatorDefinition::getIndicatorId).toList();
    List<WeeklyIndicator> allWeeklyIndicators =
        weeklyIndicatorRepository.findSeriesFrom(ticker, weeklyIds, startDate);

    // Fetch existing ASTAResults to avoid individual DB queries
    List<ASTAResults> existingResultsList =
        astaResultsRepository.findByTickerAndPriceDateGreaterThanEqual(ticker, earliestMissingDate);
    Map<LocalDate, ASTAResults> existingResultsMap =
        existingResultsList.stream()
            .collect(Collectors.toMap(ASTAResults::getPriceDate, Function.identity(), (a, b) -> a));

    List<ASTAResults> resultsToSave = new ArrayList<>();

    for (LocalDate priceDate : missingDates) {
      // Slice daily prices: latest HISTORY_WINDOW daily prices up to priceDate
      List<DailyPrice> dailyPricesUpToDate =
          allDailyPrices.stream().filter(dp -> !dp.getPriceDate().isAfter(priceDate)).toList();
      if (dailyPricesUpToDate.isEmpty()) {
        continue;
      }

      List<DailyPrice> slicedDailyPrices =
          dailyPricesUpToDate.size() > HISTORY_WINDOW
              ? dailyPricesUpToDate.subList(
                  dailyPricesUpToDate.size() - HISTORY_WINDOW, dailyPricesUpToDate.size())
              : dailyPricesUpToDate;
      List<OhlcvDTO> dailyCandles =
          slicedDailyPrices.stream().map(com.alphaflow.api.mappers.OhlcvMapper::toDTO).toList();

      // Slice daily indicators based on the start and end of slicedDailyPrices
      LocalDate dailyStart = slicedDailyPrices.getFirst().getPriceDate();
      LocalDate dailyEnd = slicedDailyPrices.getLast().getPriceDate();
      List<DailyIndicator> slicedDailyIndicators =
          allDailyIndicators.stream()
              .filter(
                  di ->
                      !di.getPriceDate().isBefore(dailyStart)
                          && !di.getPriceDate().isAfter(dailyEnd))
              .toList();
      List<IndicatorSeriesDTO> dailyIndicatorSeries =
          com.alphaflow.api.mappers.IndicatorMapper.toSeries(slicedDailyIndicators);

      // Slice weekly indicators directly: latest HISTORY_WINDOW weekly indicators up to priceDate
      List<WeeklyIndicator> weeklyIndicatorsUpToDate =
          allWeeklyIndicators.stream().filter(wi -> !wi.getPriceDate().isAfter(priceDate)).toList();
      Map<Long, List<WeeklyIndicator>> byDef =
          weeklyIndicatorsUpToDate.stream()
              .collect(Collectors.groupingBy(wi -> wi.getIndicatorDefinition().getIndicatorId()));
      List<WeeklyIndicator> slicedWeeklyIndicators = new ArrayList<>();
      for (List<WeeklyIndicator> list : byDef.values()) {
        if (list.size() > HISTORY_WINDOW) {
          slicedWeeklyIndicators.addAll(list.subList(list.size() - HISTORY_WINDOW, list.size()));
        } else {
          slicedWeeklyIndicators.addAll(list);
        }
      }
      List<IndicatorSeriesDTO> weeklyIndicatorSeries =
          com.alphaflow.api.mappers.IndicatorMapper.toSeries(slicedWeeklyIndicators);

      ASTAEvaluationContext context =
          new ASTAEvaluationContext(dailyCandles, dailyIndicatorSeries, weeklyIndicatorSeries);

      // 1. MACD (12, 26) @ TIDE (Weekly)
      CalculatedSignal macd = weeklyMacdEvaluator.evaluate(context);

      // 2. Stochastic (14, 3, 3) @ WAVE (Daily)
      CalculatedSignal stoch = dailyStochasticEvaluator.evaluate(context);

      // 3. RSI (14) @ WAVE (Daily)
      CalculatedSignal rsi = dailyRsiEvaluator.evaluate(context);

      // 4. Volume (Daily)
      CalculatedSignal volume = dailyVolumeEvaluator.evaluate(context);

      // 5. Moving Average EMA (Daily)
      CalculatedSignal ema = dailyEmaEvaluator.evaluate(context);

      // Overall consensus
      boolean doubleScreenBuy =
          TradeAction.BUY == macd.signal()
              && TradeAction.BUY == stoch.signal()
              && TradeAction.BUY == rsi.signal();
      boolean doubleScreenSell =
          TradeAction.SELL == macd.signal()
              && TradeAction.SELL == stoch.signal()
              && TradeAction.SELL == rsi.signal();

      boolean checklistBuy = TradeAction.BUY == volume.signal() && TradeAction.BUY == ema.signal();
      boolean checklistSell =
          TradeAction.SELL == volume.signal() && TradeAction.SELL == ema.signal();

      TradeAction overallSignal = TradeAction.HOLD;
      if (doubleScreenBuy && checklistBuy) {
        overallSignal = TradeAction.BUY;
      } else if (doubleScreenSell && checklistSell) {
        overallSignal = TradeAction.SELL;
      }

      ASTAResults result = existingResultsMap.get(priceDate);
      if (result == null) {
        result = ASTAResults.builder().ticker(ticker).priceDate(priceDate).build();
      }

      result.setEmaSignal(ema.signal());
      result.setEmaValue(ema.value());
      result.setMacdSignal(macd.signal());
      result.setMacdValue(macd.value());
      result.setStochasticSignal(stoch.signal());
      result.setStochasticValue(stoch.value());
      result.setRsiSignal(rsi.signal());
      result.setRsiValue(rsi.value());
      result.setVolumeSignal(volume.signal());
      result.setVolumeValue(volume.value());
      result.setOverallSignal(overallSignal);

      resultsToSave.add(result);
    }

    return astaResultsRepository.saveAll(resultsToSave);
  }

  @Transactional(readOnly = true)
  public ASTAResponseDTO getAnalysis(String symbol) {
    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    com.alphaflow.persistence.entities.DailyPrice latestPrice =
        dailyPriceRepository
            .findTopByTickerOrderByPriceDateDesc(ticker)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No daily price data found for symbol: " + symbol));
    LocalDate latestPriceDate = latestPrice.getPriceDate();

    ASTAResults res =
        astaResultsRepository
            .findTopByTickerOrderByPriceDateDesc(ticker)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Analysis result not found or stale for symbol: " + symbol));

    if (res.getPriceDate().isBefore(latestPriceDate)) {
      throw new ResourceNotFoundException("Analysis result is stale for symbol: " + symbol);
    }

    return toDTO(res, symbol);
  }

  private ASTAResponseDTO toDTO(ASTAResults res, String symbol) {
    return ASTAResponseDTO.builder()
        .symbol(symbol)
        .priceDate(res.getPriceDate())
        .emaSignal(res.getEmaSignal())
        .emaValue(res.getEmaValue())
        .macdSignal(res.getMacdSignal())
        .macdValue(res.getMacdValue())
        .stochasticSignal(res.getStochasticSignal())
        .stochasticValue(res.getStochasticValue())
        .rsiSignal(res.getRsiSignal())
        .rsiValue(res.getRsiValue())
        .volumeSignal(res.getVolumeSignal())
        .volumeValue(res.getVolumeValue())
        .overallSignal(res.getOverallSignal())
        .build();
  }
}
