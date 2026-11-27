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
import com.alphaflow.engine.calculators.ChartPatternCalculator;
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
  private ChartPatternCalculator chartPatternCalculator;
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
    chartPatternCalculator = mock(ChartPatternCalculator.class);

    pipelineConfig =
        new PipelineConfig(
            yahooFinanceDownloader,
            weeklyPriceCalculator,
            supportResistanceCalculator,
            candlestickPatternCalculator,
            indicatorCalculator,
            chartPatternCalculator);

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
    assertNotNull(pipelineConfig.getChartPatternCalculator());
    assertNotNull(pipelineConfig.getTransactionManager());

    Step step1 = pipelineConfig.downloadDailyPriceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_DOWNLOAD_DAILY_PRICE, step1.getName());

    Step step2 = pipelineConfig.calculateDailySupportResistanceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_DAILY_SUPPORT_RESISTANCE, step2.getName());

    Step step3 = pipelineConfig.calculateDailyCandlestickPatternStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_DAILY_CANDLESTICK_PATTERN, step3.getName());

    Step step4 = pipelineConfig.calculateDailyIndicatorStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_DAILY_INDICATOR, step4.getName());

    Step step5 = pipelineConfig.calculateDailyChartPatternStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_DAILY_CHART_PATTERN, step5.getName());

    Step step6 = pipelineConfig.calculateWeeklyPriceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_PRICE, step6.getName());

    Step step7 = pipelineConfig.calculateWeeklySupportResistanceStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_SUPPORT_RESISTANCE, step7.getName());

    Step step8 = pipelineConfig.calculateWeeklyCandlestickPatternStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_CANDLESTICK_PATTERN, step8.getName());

    Step step9 = pipelineConfig.calculateWeeklyIndicatorStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_INDICATOR, step9.getName());

    Step step10 = pipelineConfig.calculateWeeklyChartPatternStep(jobRepository);
    assertEquals(PipelineConfig.STEP_CALCULATE_WEEKLY_CHART_PATTERN, step10.getName());
  }

  @Test
  void testDailyPipelineJobExecutesAllConfiguredStepsInOrder() throws Exception {
    when(yahooFinanceDownloader.shouldRun(null)).thenReturn(true);
    when(supportResistanceCalculator.shouldRun(Timeframe.DAILY)).thenReturn(true);
    when(candlestickPatternCalculator.shouldRun(Timeframe.DAILY)).thenReturn(true);
    when(indicatorCalculator.shouldRun(Timeframe.DAILY)).thenReturn(true);
    when(chartPatternCalculator.shouldRun(Timeframe.DAILY)).thenReturn(true);
    when(weeklyPriceCalculator.shouldRun(null)).thenReturn(true);
    when(supportResistanceCalculator.shouldRun(Timeframe.WEEKLY)).thenReturn(true);
    when(candlestickPatternCalculator.shouldRun(Timeframe.WEEKLY)).thenReturn(true);
    when(indicatorCalculator.shouldRun(Timeframe.WEEKLY)).thenReturn(true);
    when(chartPatternCalculator.shouldRun(Timeframe.WEEKLY)).thenReturn(true);

    Step step1 = pipelineConfig.downloadDailyPriceStep(jobRepository);
    Step step2 = pipelineConfig.calculateDailySupportResistanceStep(jobRepository);
    Step step3 = pipelineConfig.calculateDailyCandlestickPatternStep(jobRepository);
    Step step4 = pipelineConfig.calculateDailyIndicatorStep(jobRepository);
    Step step5 = pipelineConfig.calculateDailyChartPatternStep(jobRepository);
    Step step6 = pipelineConfig.calculateWeeklyPriceStep(jobRepository);
    Step step7 = pipelineConfig.calculateWeeklySupportResistanceStep(jobRepository);
    Step step8 = pipelineConfig.calculateWeeklyCandlestickPatternStep(jobRepository);
    Step step9 = pipelineConfig.calculateWeeklyIndicatorStep(jobRepository);
    Step step10 = pipelineConfig.calculateWeeklyChartPatternStep(jobRepository);

    Job dailyJob =
        pipelineConfig.dailyPipelineJob(
            jobRepository, step1, step2, step3, step4, step5, step6, step7, step8, step9, step10);

    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 1000L).toJobParameters();

    JobExecution execution = jobLauncher.run(dailyJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(PipelineConfig.DAILY_PIPELINE_JOB, execution.getJobInstance().getJobName());
    assertEquals(10, execution.getStepExecutions().size());

    InOrder inOrder =
        inOrder(
            yahooFinanceDownloader,
            supportResistanceCalculator,
            candlestickPatternCalculator,
            indicatorCalculator,
            chartPatternCalculator,
            weeklyPriceCalculator);
    inOrder.verify(yahooFinanceDownloader).execute(null);
    inOrder.verify(supportResistanceCalculator).execute(Timeframe.DAILY);
    inOrder.verify(candlestickPatternCalculator).execute(Timeframe.DAILY);
    inOrder.verify(indicatorCalculator).execute(Timeframe.DAILY);
    inOrder.verify(chartPatternCalculator).execute(Timeframe.DAILY);
    inOrder.verify(weeklyPriceCalculator).execute(null);
    inOrder.verify(supportResistanceCalculator).execute(Timeframe.WEEKLY);
    inOrder.verify(candlestickPatternCalculator).execute(Timeframe.WEEKLY);
    inOrder.verify(indicatorCalculator).execute(Timeframe.WEEKLY);
    inOrder.verify(chartPatternCalculator).execute(Timeframe.WEEKLY);
  }

  @Test
  void testIntermediateStepFailureLogsFailedAndContinuesSubsequentSteps() throws Exception {
    when(yahooFinanceDownloader.shouldRun(null)).thenReturn(true);
    when(supportResistanceCalculator.shouldRun(Timeframe.DAILY)).thenReturn(true);
    when(indicatorCalculator.shouldRun(Timeframe.DAILY)).thenReturn(true);

    doThrow(new RuntimeException("Simulated SR failure"))
        .when(supportResistanceCalculator)
        .execute(Timeframe.DAILY);

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
  void testStepSkippedWhenShouldRunReturnsFalse() throws Exception {
    when(yahooFinanceDownloader.shouldRun(null)).thenReturn(false);

    Step step1 = pipelineConfig.downloadDailyPriceStep(jobRepository);
    NavigableMap<Integer, Step> steps = new TreeMap<>();
    steps.put(1, step1);

    Job testJob = pipelineConfig.buildJob("skipJob", steps, jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 7000L).toJobParameters();

    JobExecution execution = jobLauncher.run(testJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    List<StepExecution> stepExecutions = new ArrayList<>(execution.getStepExecutions());
    assertEquals(1, stepExecutions.size());
    assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getStatus());
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
