package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import com.alphaflow.engine.pipeline.PipelineJob;
import java.util.NavigableMap;
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
  public static final String STEP_CALCULATE_DAILY_INDICATOR = "CALCULATE_DAILY_INDICATOR";
  public static final String STEP_CALCULATE_WEEKLY_PRICE = "CALCULATE_WEEKLY_PRICE";
  public static final String STEP_CALCULATE_WEEKLY_SUPPORT_RESISTANCE =
      "CALCULATE_WEEKLY_SUPPORT_RESISTANCE";
  public static final String STEP_CALCULATE_WEEKLY_CANDLESTICK_PATTERN =
      "CALCULATE_WEEKLY_CANDLESTICK_PATTERN";
  public static final String STEP_CALCULATE_WEEKLY_INDICATOR = "CALCULATE_WEEKLY_INDICATOR";

  private final YahooFinanceDownloader yahooFinanceDownloader;
  private final WeeklyPriceCalculator weeklyPriceCalculator;
  private final SupportResistanceCalculator supportResistanceCalculator;
  private final CandlestickPatternCalculator candlestickPatternCalculator;
  private final IndicatorCalculator indicatorCalculator;
  private final ResourcelessTransactionManager transactionManager =
      new ResourcelessTransactionManager();

  public PipelineConfig(
      YahooFinanceDownloader yahooFinanceDownloader,
      WeeklyPriceCalculator weeklyPriceCalculator,
      SupportResistanceCalculator supportResistanceCalculator,
      CandlestickPatternCalculator candlestickPatternCalculator,
      IndicatorCalculator indicatorCalculator) {
    this.yahooFinanceDownloader = yahooFinanceDownloader;
    this.weeklyPriceCalculator = weeklyPriceCalculator;
    this.supportResistanceCalculator = supportResistanceCalculator;
    this.candlestickPatternCalculator = candlestickPatternCalculator;
    this.indicatorCalculator = indicatorCalculator;
  }

  @Bean
  public Step downloadDailyPriceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_DOWNLOAD_DAILY_PRICE,
        yahooFinanceDownloader,
        null,
        jobRepository);
  }

  @Bean
  public Step calculateDailySupportResistanceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_DAILY_SUPPORT_RESISTANCE,
        supportResistanceCalculator,
        Timeframe.DAILY,
        jobRepository);
  }

  @Bean
  public Step calculateDailyCandlestickPatternStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_DAILY_CANDLESTICK_PATTERN,
        candlestickPatternCalculator,
        Timeframe.DAILY,
        jobRepository);
  }

  @Bean
  public Step calculateDailyIndicatorStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_DAILY_INDICATOR,
        indicatorCalculator,
        Timeframe.DAILY,
        jobRepository);
  }

  @Bean
  public Step calculateWeeklyPriceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_PRICE,
        weeklyPriceCalculator,
        null,
        jobRepository);
  }

  @Bean
  public Step calculateWeeklySupportResistanceStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_SUPPORT_RESISTANCE,
        supportResistanceCalculator,
        Timeframe.WEEKLY,
        jobRepository);
  }

  @Bean
  public Step calculateWeeklyCandlestickPatternStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_CANDLESTICK_PATTERN,
        candlestickPatternCalculator,
        Timeframe.WEEKLY,
        jobRepository);
  }

  @Bean
  public Step calculateWeeklyIndicatorStep(JobRepository jobRepository) {
    return buildStep(
        STEP_CALCULATE_WEEKLY_INDICATOR,
        indicatorCalculator,
        Timeframe.WEEKLY,
        jobRepository);
  }

  @Bean(name = DAILY_PIPELINE_JOB)
  public Job dailyPipelineJob(
      JobRepository jobRepository,
      Step downloadDailyPriceStep,
      Step calculateDailySupportResistanceStep,
      Step calculateDailyCandlestickPatternStep,
      Step calculateDailyIndicatorStep,
      Step calculateWeeklyPriceStep,
      Step calculateWeeklySupportResistanceStep,
      Step calculateWeeklyCandlestickPatternStep,
      Step calculateWeeklyIndicatorStep) {
    NavigableMap<Integer, Step> steps = new TreeMap<>();
    steps.put(1, downloadDailyPriceStep);
    steps.put(2, calculateDailySupportResistanceStep);
    steps.put(3, calculateDailyCandlestickPatternStep);
    steps.put(4, calculateDailyIndicatorStep);
    steps.put(5, calculateWeeklyPriceStep);
    steps.put(6, calculateWeeklySupportResistanceStep);
    steps.put(7, calculateWeeklyCandlestickPatternStep);
    steps.put(8, calculateWeeklyIndicatorStep);
    return buildJob(DAILY_PIPELINE_JOB, steps, jobRepository);
  }

  /**
   * Chains a NavigableMap of steps ordered explicitly by their sequence key with fail-safe
   * .on("*").to(...) transitions.
   */
  public Job buildJob(
      String jobName, NavigableMap<Integer, Step> stepsByOrder, JobRepository jobRepository) {
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
   * Builds an in-memory tasklet step that checks if the pipeline job should run before executing.
   */
  public Step buildStep(
      String stepName, PipelineJob job, Timeframe timeframe, JobRepository jobRepository) {
    return new StepBuilder(stepName, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              long startTime = System.currentTimeMillis();
              log.info("Checking if pipeline step {} should run...", stepName);
              if (job.shouldRun(timeframe)) {
                log.info("Starting pipeline step {}...", stepName);
                try {
                  job.execute(timeframe);
                  log.info(
                      "Pipeline step {} COMPLETED in {}.",
                      stepName,
                      DurationFormatUtils.formatDurationWords(
                          System.currentTimeMillis() - startTime, true, false));
                } catch (Exception exception) {
                  log.error(
                      "Pipeline step {} FAILED after {}.",
                      stepName,
                      DurationFormatUtils.formatDurationWords(
                          System.currentTimeMillis() - startTime, true, false),
                      exception);
                  throw exception;
                }
              } else {
                log.info("Pipeline step {} skipped: all active tickers are up to date.", stepName);
              }
              return RepeatStatus.FINISHED;
            },
            transactionManager)
        .build();
  }
}
