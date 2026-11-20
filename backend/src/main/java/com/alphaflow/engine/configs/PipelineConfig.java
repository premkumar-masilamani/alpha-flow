package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.ChartPatternCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import java.util.TreeMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures and assembles in-memory Spring Batch pipeline jobs and steps for daily and weekly
 * technical analysis execution flows.
 */
@Configuration
@Slf4j
@Getter
public class PipelineConfig {

  public static final String DAILY_PIPELINE_JOB = "dailyPipelineJob";

  public static final String STEP_DOWNLOAD_DAILY_PRICE = "DOWNLOAD_DAILY_PRICE";
  public static final String STEP_CALCULATE_DAILY_SUPPORT_RESISTANCE =
      "CALCULATE_DAILY_SUPPORT_RESISTANCE";
  public static final String STEP_CALCULATE_DAILY_CANDLESTICK_PATTERN =
      "CALCULATE_DAILY_CANDLESTICK_PATTERN";
  public static final String STEP_CALCULATE_DAILY_CHART_PATTERN =
      "CALCULATE_DAILY_CHART_PATTERN";
  public static final String STEP_CALCULATE_DAILY_INDICATOR = "CALCULATE_DAILY_INDICATOR";
  public static final String STEP_CALCULATE_WEEKLY_PRICE = "CALCULATE_WEEKLY_PRICE";
  public static final String STEP_CALCULATE_WEEKLY_SUPPORT_RESISTANCE =
      "CALCULATE_WEEKLY_SUPPORT_RESISTANCE";
  public static final String STEP_CALCULATE_WEEKLY_CANDLESTICK_PATTERN =
      "CALCULATE_WEEKLY_CANDLESTICK_PATTERN";
  public static final String STEP_CALCULATE_WEEKLY_CHART_PATTERN =
      "CALCULATE_WEEKLY_CHART_PATTERN";
  public static final String STEP_CALCULATE_WEEKLY_INDICATOR = "CALCULATE_WEEKLY_INDICATOR";

  private final YahooFinanceDownloader yahooFinanceDownloader;
  private final WeeklyPriceCalculator weeklyPriceCalculator;
  private final SupportResistanceCalculator supportResistanceCalculator;
  private final CandlestickPatternCalculator candlestickPatternCalculator;
  private final ChartPatternCalculator chartPatternCalculator;
  private final IndicatorCalculator indicatorCalculator;
  private final ResourcelessTransactionManager transactionManager =
      new ResourcelessTransactionManager();

  public PipelineConfig(
      YahooFinanceDownloader yahooFinanceDownloader,
      WeeklyPriceCalculator weeklyPriceCalculator,
      SupportResistanceCalculator supportResistanceCalculator,
      CandlestickPatternCalculator candlestickPatternCalculator,
      ChartPatternCalculator chartPatternCalculator,
      IndicatorCalculator indicatorCalculator) {
    this.yahooFinanceDownloader = yahooFinanceDownloader;
    this.weeklyPriceCalculator = weeklyPriceCalculator;
    this.supportResistanceCalculator = supportResistanceCalculator;
    this.candlestickPatternCalculator = candlestickPatternCalculator;
    this.chartPatternCalculator = chartPatternCalculator;
    this.indicatorCalculator = indicatorCalculator;
  }

  @Bean
  public Step downloadDailyPriceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_DOWNLOAD_DAILY_PRICE,
            yahooFinanceDownloader::downloadAndPersistDailyPrices,
        jobRepository);
  }

  @Bean
  public Step calculateDailySupportResistanceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_DAILY_SUPPORT_RESISTANCE,
        () -> supportResistanceCalculator.computeSupportResistances(Timeframe.DAILY),
        jobRepository);
  }

  @Bean
  public Step calculateDailyCandlestickPatternStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_DAILY_CANDLESTICK_PATTERN,
        () -> candlestickPatternCalculator.computeCandleStickPatterns(Timeframe.DAILY),
        jobRepository);
  }

  @Bean
  public Step calculateDailyChartPatternStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_DAILY_CHART_PATTERN,
        () -> chartPatternCalculator.computeChartPatterns(Timeframe.DAILY),
        jobRepository);
  }

  @Bean
  public Step calculateDailyIndicatorStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_DAILY_INDICATOR,
        () -> indicatorCalculator.computeIndicators(Timeframe.DAILY),
        jobRepository);
  }

  @Bean
  public Step calculateWeeklyPriceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_PRICE,
            weeklyPriceCalculator::computeWeeklyPrices,
        jobRepository);
  }

  @Bean
  public Step calculateWeeklySupportResistanceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_SUPPORT_RESISTANCE,
        () -> supportResistanceCalculator.computeSupportResistances(Timeframe.WEEKLY),
        jobRepository);
  }

  @Bean
  public Step calculateWeeklyCandlestickPatternStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_CANDLESTICK_PATTERN,
        () -> candlestickPatternCalculator.computeCandleStickPatterns(Timeframe.WEEKLY),
        jobRepository);
  }

  @Bean
  public Step calculateWeeklyChartPatternStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_CHART_PATTERN,
        () -> chartPatternCalculator.computeChartPatterns(Timeframe.WEEKLY),
        jobRepository);
  }

  @Bean
  public Step calculateWeeklyIndicatorStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_INDICATOR,
        () -> indicatorCalculator.computeIndicators(Timeframe.WEEKLY),
        jobRepository);
  }

  @Bean(name = DAILY_PIPELINE_JOB)
  public Job dailyPipelineJob(
      JobRepository jobRepository,
      Step downloadDailyPriceStep,
      Step calculateDailySupportResistanceStep,
      Step calculateDailyCandlestickPatternStep,
      Step calculateDailyChartPatternStep,
      Step calculateDailyIndicatorStep,
      Step calculateWeeklyPriceStep,
      Step calculateWeeklySupportResistanceStep,
      Step calculateWeeklyCandlestickPatternStep,
      Step calculateWeeklyChartPatternStep,
      Step calculateWeeklyIndicatorStep) {
    TreeMap<Integer, Step> steps = new TreeMap<>();
    steps.put(1, downloadDailyPriceStep);
    steps.put(2, calculateDailySupportResistanceStep);
    steps.put(3, calculateDailyCandlestickPatternStep);
    steps.put(4, calculateDailyChartPatternStep);
    steps.put(5, calculateDailyIndicatorStep);
    steps.put(6, calculateWeeklyPriceStep);
    steps.put(7, calculateWeeklySupportResistanceStep);
    steps.put(8, calculateWeeklyCandlestickPatternStep);
    steps.put(9, calculateWeeklyChartPatternStep);
    steps.put(10, calculateWeeklyIndicatorStep);
    return buildJob(DAILY_PIPELINE_JOB, steps, jobRepository);
  }

  /**
   * Chains a TreeMap of steps ordered explicitly by their sequence key with fail-safe .on("*").to(...)
   * transitions.
   */
  public Job buildJob(
      String jobName, TreeMap<Integer, Step> stepsByOrder, JobRepository jobRepository) {
    if (stepsByOrder != null && !stepsByOrder.isEmpty()) {
      Step firstStep = stepsByOrder.pollFirstEntry().getValue();
      JobFlowBuilder flowBuilder = new JobBuilder(jobName, jobRepository).flow(firstStep);
      for (Step step : stepsByOrder.values()) {
        flowBuilder.on("*").to(step);
      }
      return flowBuilder.end().build();
    } else {
      throw new IllegalArgumentException("Pipeline steps map must not be null or empty");
    }
  }

  /**
   * Builds an in-memory tasklet step that executes an action and logs timing.
   */
  public Step buildStep(String stepName, Runnable action, JobRepository jobRepository) {
    return new StepBuilder(stepName, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              long startTime = System.currentTimeMillis();
              log.info("Starting pipeline step {}...", stepName);
              try {
                action.run();
                log.info(
                    "Pipeline step {} COMPLETED in {}.",
                    stepName,
                    DurationFormatUtils.formatDurationWords(
                        System.currentTimeMillis() - startTime, true, false));
                return RepeatStatus.FINISHED;
              } catch (Exception exception) {
                log.error(
                    "Pipeline step {} FAILED after {}.",
                    stepName,
                    DurationFormatUtils.formatDurationWords(
                        System.currentTimeMillis() - startTime, true, false),
                    exception);
                throw exception;
              }
            },
            transactionManager)
        .build();
  }
}
