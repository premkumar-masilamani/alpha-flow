package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;

@SuppressWarnings("removal")
class PipelineConfigTest {

  private YahooFinanceDownloader yahooFinanceDownloader;
  private WeeklyPriceCalculator weeklyPriceCalculator;
  private SupportResistanceCalculator supportResistanceCalculator;
  private CandlestickPatternCalculator candlestickPatternCalculator;
  private IndicatorCalculator indicatorCalculator;
  private PipelineConfig pipelineConfig;
  private JobRepository jobRepository;
  private TaskExecutorJobLauncher jobLauncher;

  @BeforeEach
  void setUp() throws Exception {
    yahooFinanceDownloader = mock(YahooFinanceDownloader.class);
    weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    supportResistanceCalculator = mock(SupportResistanceCalculator.class);
    candlestickPatternCalculator = mock(CandlestickPatternCalculator.class);
    indicatorCalculator = mock(IndicatorCalculator.class);

    pipelineConfig =
        new PipelineConfig(
            yahooFinanceDownloader,
            weeklyPriceCalculator,
            supportResistanceCalculator,
            candlestickPatternCalculator,
            indicatorCalculator);

    jobRepository = new ResourcelessJobRepository();
    jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.afterPropertiesSet();
  }

  @Test
  void testStepBeansCreationAndGetters() {
    assertNotNull(pipelineConfig.getYahooFinanceDownloader());
    assertNotNull(pipelineConfig.getWeeklyPriceCalculator());
    assertNotNull(pipelineConfig.getSupportResistanceCalculator());
    assertNotNull(pipelineConfig.getCandlestickPatternCalculator());
    assertNotNull(pipelineConfig.getIndicatorCalculator());
    assertNotNull(pipelineConfig.getTransactionManager());

    Step step1 = pipelineConfig.downloadDailyPriceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_DOWNLOAD_DAILY_PRICE, step1.getName());

    Step step2 = pipelineConfig.calculateDailySupportResistanceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_DAILY_SUPPORT_RESISTANCE, step2.getName());

    Step step3 = pipelineConfig.calculateDailyCandlestickPatternStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_DAILY_CANDLESTICK_PATTERN, step3.getName());

    Step step4 = pipelineConfig.calculateDailyIndicatorStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_DAILY_INDICATOR, step4.getName());

    Step step5 = pipelineConfig.calculateWeeklyPriceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_PRICE, step5.getName());

    Step step6 = pipelineConfig.calculateWeeklySupportResistanceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_SUPPORT_RESISTANCE, step6.getName());

    Step step7 = pipelineConfig.calculateWeeklyCandlestickPatternStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_CANDLESTICK_PATTERN, step7.getName());

    Step step8 = pipelineConfig.calculateWeeklyIndicatorStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_INDICATOR, step8.getName());
  }

  @Test
  void testDailyPipelineJobExecutesAllConfiguredStepsInOrder() throws Exception {
    when(yahooFinanceDownloader.downloadAndPersistDailyPrices()).thenReturn(5);

    Step step1 = pipelineConfig.downloadDailyPriceStep(jobRepository);
    Step step2 = pipelineConfig.calculateDailySupportResistanceStep(jobRepository);
    Step step3 = pipelineConfig.calculateDailyCandlestickPatternStep(jobRepository);
    Step step4 = pipelineConfig.calculateDailyIndicatorStep(jobRepository);
    Step step5 = pipelineConfig.calculateWeeklyPriceStep(jobRepository);
    Step step6 = pipelineConfig.calculateWeeklySupportResistanceStep(jobRepository);
    Step step7 = pipelineConfig.calculateWeeklyCandlestickPatternStep(jobRepository);
    Step step8 = pipelineConfig.calculateWeeklyIndicatorStep(jobRepository);

    Job dailyJob =
        pipelineConfig.dailyPipelineJob(
            jobRepository, step1, step2, step3, step4, step5, step6, step7, step8);

    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 1000L).toJobParameters();

    JobExecution execution = jobLauncher.run(dailyJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(PipelineConfig.DAILY_PIPELINE_JOB, execution.getJobInstance().getJobName());
    assertEquals(8, execution.getStepExecutions().size());

    InOrder inOrder =
        inOrder(
            yahooFinanceDownloader,
            supportResistanceCalculator,
            candlestickPatternCalculator,
            indicatorCalculator,
            weeklyPriceCalculator);
    inOrder.verify(yahooFinanceDownloader).downloadAndPersistDailyPrices();
    inOrder.verify(supportResistanceCalculator).computeSupportResistances(Timeframe.DAILY);
    inOrder.verify(candlestickPatternCalculator).computeCandleStickPatterns(Timeframe.DAILY);
    inOrder.verify(indicatorCalculator).computeIndicators(Timeframe.DAILY);
    inOrder.verify(weeklyPriceCalculator).computeWeeklyPrices();
    inOrder.verify(supportResistanceCalculator).computeSupportResistances(Timeframe.WEEKLY);
    inOrder.verify(candlestickPatternCalculator).computeCandleStickPatterns(Timeframe.WEEKLY);
    inOrder.verify(indicatorCalculator).computeIndicators(Timeframe.WEEKLY);
  }

  @Test
  void testIntermediateStepFailureLogsFailedAndContinuesSubsequentSteps() throws Exception {
    doThrow(new RuntimeException("Simulated SR failure"))
        .when(supportResistanceCalculator)
        .computeSupportResistances(Timeframe.DAILY);

    Step step1 = pipelineConfig.downloadDailyPriceStep(jobRepository);
    Step step2 = pipelineConfig.calculateDailySupportResistanceStep(jobRepository);
    Step step3 = pipelineConfig.calculateDailyIndicatorStep(jobRepository);

    // Provide steps out-of-order in the TreeMap to verify explicit numeric key sorting (1, 2, 3)
    NavigableMap<Integer, Step> steps = new TreeMap<>();
    steps.put(3, step3);
    steps.put(1, step1);
    steps.put(2, step2);

    Job testJob = pipelineConfig.buildJob("testJob", steps, jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 5000L).toJobParameters();

    JobExecution execution = jobLauncher.run(testJob, params);

    List<StepExecution> stepExecutions = new ArrayList<>(execution.getStepExecutions());
    assertEquals(3, stepExecutions.size());
    assertEquals(PipelineConfig.STEP_DOWNLOAD_DAILY_PRICE, stepExecutions.get(0).getStepName());
    assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getStatus());
    assertEquals(
        PipelineConfig.STEP_CALCULATE_DAILY_SUPPORT_RESISTANCE,
        stepExecutions.get(1).getStepName());
    assertEquals(BatchStatus.ABANDONED, stepExecutions.get(1).getStatus());
    assertEquals(
        PipelineConfig.STEP_CALCULATE_DAILY_INDICATOR, stepExecutions.get(2).getStepName());
    assertEquals(BatchStatus.COMPLETED, stepExecutions.get(2).getStatus());
  }

  @Test
  void testBuildJobThrowsWhenMapNullOrEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> pipelineConfig.buildJob("invalidJob", null, jobRepository));
    assertThrows(
        IllegalArgumentException.class,
        () -> pipelineConfig.buildJob("invalidJob", new TreeMap<>(), jobRepository));
  }
}
