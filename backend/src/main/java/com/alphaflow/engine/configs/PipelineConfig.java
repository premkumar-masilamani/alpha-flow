package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.ChartPatternCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import com.alphaflow.engine.pipeline.enums.PipelineStepName;
import com.alphaflow.engine.schedulers.SchedulerUtils;
import com.alphaflow.engine.strategies.Intraday70PBounceStrategy;
import jakarta.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures and dynamically assembles in-memory Spring Batch pipeline jobs for daily, weekly, and
 * intraday execution flows based on ordered step lists in {@code application.properties}.
 */
@Configuration
@Slf4j
@Getter
@Setter
public class PipelineConfig {

  public static final String DAILY_STEPS_PROPERTY = "alphaflow.pipeline.daily.steps";
  public static final String INTRADAY_CANDLES_STEPS_PROPERTY =
      "alphaflow.pipeline.intraday-candles.steps";
  public static final String LIVE_QUOTES_STEPS_PROPERTY = "alphaflow.pipeline.live-quotes.steps";

  @Value("${alphaflow.pipeline.daily.steps}")
  private String dailySteps;

  @Value("${alphaflow.pipeline.intraday-candles.steps}")
  private String intradayCandlesSteps;

  @Value("${alphaflow.pipeline.live-quotes.steps}")
  private String liveQuotesSteps;

  private final ResourcelessTransactionManager transactionManager =
      new ResourcelessTransactionManager();
  private final Map<PipelineStepName, Runnable> stepActions;

  public PipelineConfig(
      YahooFinanceDownloader yahooFinanceDownloader,
      WeeklyPriceCalculator weeklyPriceCalculator,
      SupportResistanceCalculator supportResistanceCalculator,
      CandlestickPatternCalculator candlestickPatternCalculator,
      ChartPatternCalculator chartPatternCalculator,
      IndicatorCalculator indicatorCalculator,
      AngelOneDownloader angelOneDownloader,
      Intraday70PBounceStrategy intraday70PBounceStrategy) {
    Map<PipelineStepName, Runnable> actions = new EnumMap<>(PipelineStepName.class);
    actions.put(
        PipelineStepName.YAHOO_DAILY_DOWNLOAD,
        yahooFinanceDownloader::downloadAndPersistDailyPrices);
    actions.put(
        PipelineStepName.DAILY_SUPPORT_RESISTANCE,
        () -> supportResistanceCalculator.computeSupportResistances(Timeframe.DAILY));
    actions.put(
        PipelineStepName.DAILY_CANDLESTICK_PATTERN,
        () -> candlestickPatternCalculator.computeCandleStickPatterns(Timeframe.DAILY));
    actions.put(
        PipelineStepName.DAILY_CHART_PATTERN,
        () -> chartPatternCalculator.computeChartPatterns(Timeframe.DAILY));
    actions.put(
        PipelineStepName.DAILY_INDICATOR,
        () -> indicatorCalculator.computeIndicators(Timeframe.DAILY));
    actions.put(PipelineStepName.WEEKLY_PRICE, weeklyPriceCalculator::computeWeeklyPrices);
    actions.put(
        PipelineStepName.WEEKLY_SUPPORT_RESISTANCE,
        () -> supportResistanceCalculator.computeSupportResistances(Timeframe.WEEKLY));
    actions.put(
        PipelineStepName.WEEKLY_CANDLESTICK_PATTERN,
        () -> candlestickPatternCalculator.computeCandleStickPatterns(Timeframe.WEEKLY));
    actions.put(
        PipelineStepName.WEEKLY_CHART_PATTERN,
        () -> chartPatternCalculator.computeChartPatterns(Timeframe.WEEKLY));
    actions.put(
        PipelineStepName.WEEKLY_INDICATOR,
        () -> indicatorCalculator.computeIndicators(Timeframe.WEEKLY));
    actions.put(
        PipelineStepName.ANGEL_ONE_INTRADAY_DOWNLOAD,
        angelOneDownloader::downloadAndPersistIntradayPrices);
    actions.put(
        PipelineStepName.ANGEL_ONE_LIVE_QUOTE_DOWNLOAD,
        () ->
            angelOneDownloader.downloadLiveQuotes(
                ZonedDateTime.now(MarketConfig.IST_ZONE).toOffsetDateTime()));
    actions.put(
        PipelineStepName.INTRADAY_70P_BOUNCE_STRATEGY,
        intraday70PBounceStrategy::executeForAllActiveQuotes);
    this.stepActions = actions;
  }

  /**
   * Validates all pipeline step configuration properties on startup, failing fast if any property
   * is missing, blank, unexpanded, or contains an unrecognized step identifier.
   */
  @PostConstruct
  public void validate() {
    parseSteps(DAILY_STEPS_PROPERTY, dailySteps);
    parseSteps(INTRADAY_CANDLES_STEPS_PROPERTY, intradayCandlesSteps);
    parseSteps(LIVE_QUOTES_STEPS_PROPERTY, liveQuotesSteps);
  }

  /**
   * Parses a comma-separated pipeline steps property value into an ordered list of {@link
   * PipelineStepName} values.
   *
   * @param propertyName the configuration property key used for diagnostic error reporting
   * @param rawSteps the raw comma-separated step string
   * @return an immutable ordered list of {@link PipelineStepName}
   */
  public List<PipelineStepName> parseSteps(String propertyName, String rawSteps) {
    if (StringUtils.isNotBlank(rawSteps) && !rawSteps.contains("${")) {
      String[] tokens = rawSteps.split(",");
      List<PipelineStepName> stepNames = new ArrayList<>(tokens.length);
      for (String token : tokens) {
        stepNames.add(PipelineStepName.from(token));
      }
      return List.copyOf(stepNames);
    } else {
      throw new IllegalArgumentException(
          "Required configuration property '"
              + propertyName
              + "' is missing, blank, or unexpanded");
    }
  }

  @Bean
  public Job dailyPipelineJob(JobRepository jobRepository) {
    return buildJob(
        "dailyPipelineJob", parseSteps(DAILY_STEPS_PROPERTY, dailySteps), jobRepository);
  }

  @Bean
  public Job intradayCandlePipelineJob(JobRepository jobRepository) {
    return buildJob(
        "intradayCandlePipelineJob",
        parseSteps(INTRADAY_CANDLES_STEPS_PROPERTY, intradayCandlesSteps),
        jobRepository);
  }

  @Bean
  public Job liveQuotePipelineJob(JobRepository jobRepository) {
    return buildJob(
        "liveQuotePipelineJob",
        parseSteps(LIVE_QUOTES_STEPS_PROPERTY, liveQuotesSteps),
        jobRepository);
  }

  private Job buildJob(
      String jobName, List<PipelineStepName> stepNames, JobRepository jobRepository) {
    Step firstStep = createStep(stepNames.get(0), jobRepository);
    JobFlowBuilder flowBuilder = new JobBuilder(jobName, jobRepository).flow(firstStep);
    for (int index = 1; index < stepNames.size(); index++) {
      Step nextStep = createStep(stepNames.get(index), jobRepository);
      flowBuilder.on("*").to(nextStep);
    }
    return flowBuilder.end().build();
  }

  private Step createStep(PipelineStepName stepName, JobRepository jobRepository) {
    Runnable stepAction = stepActions.get(stepName);
    return new StepBuilder(stepName.name(), jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              long startTime = System.currentTimeMillis();
              log.info("Starting pipeline step {}...", stepName);
              try {
                stepAction.run();
                log.info(
                    "Pipeline step {} COMPLETED in {}.",
                    stepName,
                    SchedulerUtils.formatDuration(System.currentTimeMillis() - startTime));
                return RepeatStatus.FINISHED;
              } catch (Exception exception) {
                log.error(
                    "Pipeline step {} FAILED after {}.",
                    stepName,
                    SchedulerUtils.formatDuration(System.currentTimeMillis() - startTime),
                    exception);
                throw exception;
              }
            },
            transactionManager)
        .build();
  }
}
