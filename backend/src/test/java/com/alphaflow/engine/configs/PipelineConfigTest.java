package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.ChartPatternCalculator;
import com.alphaflow.engine.calculators.DailyPriceFromIntradayCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import com.alphaflow.engine.pipeline.enums.PipelineStepName;
import com.alphaflow.engine.strategies.Intraday70PBounceStrategy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.batch.core.step.StepExecution;

@SuppressWarnings("removal")
class PipelineConfigTest {

  private YahooFinanceDownloader yahooFinanceDownloader;
  private WeeklyPriceCalculator weeklyPriceCalculator;
  private SupportResistanceCalculator supportResistanceCalculator;
  private CandlestickPatternCalculator candlestickPatternCalculator;
  private ChartPatternCalculator chartPatternCalculator;
  private IndicatorCalculator indicatorCalculator;
  private DailyPriceFromIntradayCalculator dailyPriceFromIntradayCalculator;
  private AngelOneDownloader angelOneDownloader;
  private Intraday70PBounceStrategy intraday70PBounceStrategy;
  private PipelineConfig pipelineConfig;
  private JobRepository jobRepository;
  private TaskExecutorJobLauncher jobLauncher;

  @BeforeEach
  void setUp() throws Exception {
    yahooFinanceDownloader = mock(YahooFinanceDownloader.class);
    weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    supportResistanceCalculator = mock(SupportResistanceCalculator.class);
    candlestickPatternCalculator = mock(CandlestickPatternCalculator.class);
    chartPatternCalculator = mock(ChartPatternCalculator.class);
    indicatorCalculator = mock(IndicatorCalculator.class);
    dailyPriceFromIntradayCalculator = mock(DailyPriceFromIntradayCalculator.class);
    angelOneDownloader = mock(AngelOneDownloader.class);
    intraday70PBounceStrategy = mock(Intraday70PBounceStrategy.class);

    pipelineConfig =
        new PipelineConfig(
            yahooFinanceDownloader,
            weeklyPriceCalculator,
            supportResistanceCalculator,
            candlestickPatternCalculator,
            chartPatternCalculator,
            indicatorCalculator,
            dailyPriceFromIntradayCalculator,
            angelOneDownloader,
            intraday70PBounceStrategy);

    pipelineConfig.setDailySteps(
        "DOWNLOAD_DAILY_YAHOO_FINANCE, CALCULATE_DAILY_SUPPORT_RESISTANCE, CALCULATE_DAILY_CANDLESTICK_PATTERN, "
            + "CALCULATE_DAILY_INDICATOR, CALCULATE_WEEKLY_PRICE, CALCULATE_WEEKLY_SUPPORT_RESISTANCE, "
            + "CALCULATE_WEEKLY_CANDLESTICK_PATTERN, CALCULATE_WEEKLY_CHART_PATTERN, CALCULATE_WEEKLY_INDICATOR");
    pipelineConfig.setIntradayCandlesSteps("DOWNLOAD_INTRADAY_ANGEL_ONE");
    pipelineConfig.setLiveQuotesSteps(
        "DOWNLOAD_INTRADAY_ANGEL_ONE_LIVE_QUOTE, STRATEGY_INTRADAY_70P_BOUNCE");
    pipelineConfig.setTechnicalAnalysisSteps(
        "CALCULATE_SUPPORT_RESISTANCE, CALCULATE_CANDLESTICK_PATTERN, CALCULATE_INDICATOR");

    jobRepository = new ResourcelessJobRepository();
    jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.afterPropertiesSet();
  }

  @Test
  void testValidateAndGettersSucceedWithDefaultConfiguration() {
    pipelineConfig.validate();

    assertNotNull(pipelineConfig.getDailySteps());
    assertNotNull(pipelineConfig.getIntradayCandlesSteps());
    assertNotNull(pipelineConfig.getLiveQuotesSteps());
    assertNotNull(pipelineConfig.getTechnicalAnalysisSteps());
    assertNotNull(pipelineConfig.getTransactionManager());
    assertEquals(14, pipelineConfig.getStepActions().size());
    assertEquals(4, pipelineConfig.getParameterizedStepActions().size());
  }

  @Test
  void testParseStepsFailsFastWhenNullBlankUnexpandedOrUnknown() {
    assertThrows(
        IllegalArgumentException.class,
        () -> pipelineConfig.parseSteps(PipelineConfig.DAILY_STEPS_PROPERTY, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> pipelineConfig.parseSteps(PipelineConfig.DAILY_STEPS_PROPERTY, ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> pipelineConfig.parseSteps(PipelineConfig.DAILY_STEPS_PROPERTY, "   "));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            pipelineConfig.parseSteps(
                PipelineConfig.DAILY_STEPS_PROPERTY, "${alphaflow.pipeline.daily.steps}"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            pipelineConfig.parseSteps(
                PipelineConfig.DAILY_STEPS_PROPERTY, "DOWNLOAD_DAILY_YAHOO_FINANCE, UNKNOWN_STEP"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            pipelineConfig.parseSteps(
                PipelineConfig.TECHNICAL_ANALYSIS_STEPS_PROPERTY, "${alphaflow.pipeline.technical-analysis.steps}"));
  }

  @Test
  void testDailyPipelineJobExecutesAllConfiguredStepsInOrderEvenWithZeroNewRows()
      throws Exception {
    when(yahooFinanceDownloader.downloadAndPersistDailyPrices()).thenReturn(0);

    Job dailyJob = pipelineConfig.dailyPipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 1000L).toJobParameters();

    JobExecution execution = jobLauncher.run(dailyJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(9, execution.getStepExecutions().size());

    InOrder inOrder =
        inOrder(
            yahooFinanceDownloader,
            supportResistanceCalculator,
            candlestickPatternCalculator,
            indicatorCalculator,
            weeklyPriceCalculator,
            chartPatternCalculator);
    inOrder.verify(yahooFinanceDownloader).downloadAndPersistDailyPrices();
    inOrder.verify(supportResistanceCalculator).computeSupportResistances(Timeframe.DAILY);
    inOrder.verify(candlestickPatternCalculator).computeCandleStickPatterns(Timeframe.DAILY);
    inOrder.verify(indicatorCalculator).computeIndicators(Timeframe.DAILY);
    inOrder.verify(weeklyPriceCalculator).computeWeeklyPrices();
    inOrder.verify(supportResistanceCalculator).computeSupportResistances(Timeframe.WEEKLY);
    inOrder.verify(candlestickPatternCalculator).computeCandleStickPatterns(Timeframe.WEEKLY);
    inOrder.verify(chartPatternCalculator).computeChartPatterns(Timeframe.WEEKLY);
    inOrder.verify(indicatorCalculator).computeIndicators(Timeframe.WEEKLY);

    verify(chartPatternCalculator, never()).computeChartPatterns(Timeframe.DAILY);
  }

  @Test
  void testDailyPipelineJobExecutesDailyChartPatternWhenIncluded() throws Exception {
    pipelineConfig.setDailySteps("CALCULATE_DAILY_CHART_PATTERN, CALCULATE_WEEKLY_CHART_PATTERN");
    Job dailyJob = pipelineConfig.dailyPipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 2000L).toJobParameters();

    JobExecution execution = jobLauncher.run(dailyJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    verify(chartPatternCalculator, times(1)).computeChartPatterns(Timeframe.DAILY);
    verify(chartPatternCalculator, times(1)).computeChartPatterns(Timeframe.WEEKLY);
  }

  @Test
  void testIntradayCandlePipelineJobExecutesDownloadStep() throws Exception {
    Job candleJob = pipelineConfig.intradayCandlePipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 3000L).toJobParameters();

    JobExecution execution = jobLauncher.run(candleJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(1, execution.getStepExecutions().size());
    verify(angelOneDownloader, times(1)).downloadAndPersistIntradayPrices();
  }

  @Test
  void testLiveQuotePipelineJobExecutesQuoteDownloadThenStrategyInOrder() throws Exception {
    Job liveQuoteJob = pipelineConfig.liveQuotePipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 4000L).toJobParameters();

    JobExecution execution = jobLauncher.run(liveQuoteJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(2, execution.getStepExecutions().size());

    InOrder inOrder = inOrder(angelOneDownloader, intraday70PBounceStrategy);
    inOrder.verify(angelOneDownloader).downloadLiveQuotes(any(OffsetDateTime.class));
    inOrder.verify(intraday70PBounceStrategy).executeForAllActiveQuotes();
  }

  @Test
  void testTechnicalAnalysisPipelineJobExecutesWithDailyTimeframe() throws Exception {
    Job taJob = pipelineConfig.technicalAnalysisPipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder()
            .addLong("runTimestamp", 5000L)
            .addString("timeframe", "DAILY")
            .toJobParameters();

    JobExecution execution = jobLauncher.run(taJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(3, execution.getStepExecutions().size());

    InOrder inOrder = inOrder(supportResistanceCalculator, candlestickPatternCalculator, indicatorCalculator);
    inOrder.verify(supportResistanceCalculator).computeSupportResistances(Timeframe.DAILY);
    inOrder.verify(candlestickPatternCalculator).computeCandleStickPatterns(Timeframe.DAILY);
    inOrder.verify(indicatorCalculator).computeIndicators(Timeframe.DAILY);
  }

  @Test
  void testTechnicalAnalysisPipelineJobExecutesWithWeeklyTimeframe() throws Exception {
    pipelineConfig.setTechnicalAnalysisSteps(
        "CALCULATE_SUPPORT_RESISTANCE, CALCULATE_CANDLESTICK_PATTERN, CALCULATE_CHART_PATTERN, CALCULATE_INDICATOR");
    Job taJob = pipelineConfig.technicalAnalysisPipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder()
            .addLong("runTimestamp", 6000L)
            .addString("timeframe", "WEEKLY")
            .toJobParameters();

    JobExecution execution = jobLauncher.run(taJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(4, execution.getStepExecutions().size());

    InOrder inOrder =
        inOrder(
            supportResistanceCalculator,
            candlestickPatternCalculator,
            chartPatternCalculator,
            indicatorCalculator);
    inOrder.verify(supportResistanceCalculator).computeSupportResistances(Timeframe.WEEKLY);
    inOrder.verify(candlestickPatternCalculator).computeCandleStickPatterns(Timeframe.WEEKLY);
    inOrder.verify(chartPatternCalculator).computeChartPatterns(Timeframe.WEEKLY);
    inOrder.verify(indicatorCalculator).computeIndicators(Timeframe.WEEKLY);
  }

  @Test
  void testTechnicalAnalysisPipelineJobDefaultsToDailyWhenTimeframeOmitted() throws Exception {
    Job taJob = pipelineConfig.technicalAnalysisPipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 7000L).toJobParameters();

    JobExecution execution = jobLauncher.run(taJob, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    verify(supportResistanceCalculator).computeSupportResistances(Timeframe.DAILY);
  }

  @Test
  void testDailyPriceFromIntradayJobExecutesCalculator() throws Exception {
    Job job = pipelineConfig.dailyPriceFromIntradayJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 8000L).toJobParameters();

    JobExecution execution = jobLauncher.run(job, params);

    assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    assertEquals(1, execution.getStepExecutions().size());
    verify(dailyPriceFromIntradayCalculator, times(1)).computeDailyPricesFromIntraday();
  }

  @Test
  void testIntermediateStepFailureLogsFailedAndContinuesSubsequentSteps() throws Exception {
    pipelineConfig.setDailySteps(
        "DOWNLOAD_DAILY_YAHOO_FINANCE, CALCULATE_DAILY_SUPPORT_RESISTANCE, CALCULATE_DAILY_INDICATOR");
    doThrow(new RuntimeException("Simulated SR failure"))
        .when(supportResistanceCalculator)
        .computeSupportResistances(Timeframe.DAILY);

    Job dailyJob = pipelineConfig.dailyPipelineJob(jobRepository);
    JobParameters params =
        new JobParametersBuilder().addLong("runTimestamp", 9000L).toJobParameters();

    JobExecution execution = jobLauncher.run(dailyJob, params);

    List<StepExecution> stepExecutions = new ArrayList<>(execution.getStepExecutions());
    assertEquals(3, stepExecutions.size());
    assertEquals(
        PipelineStepName.DOWNLOAD_DAILY_YAHOO_FINANCE.name(), stepExecutions.get(0).getStepName());
    assertEquals(BatchStatus.COMPLETED, stepExecutions.get(0).getStatus());
    assertEquals(
        PipelineStepName.CALCULATE_DAILY_SUPPORT_RESISTANCE.name(),
        stepExecutions.get(1).getStepName());
    assertEquals(BatchStatus.ABANDONED, stepExecutions.get(1).getStatus());
    assertEquals(
        PipelineStepName.CALCULATE_DAILY_INDICATOR.name(), stepExecutions.get(2).getStepName());
    assertEquals(BatchStatus.COMPLETED, stepExecutions.get(2).getStatus());

    verify(yahooFinanceDownloader, times(1)).downloadAndPersistDailyPrices();
    verify(supportResistanceCalculator, times(1)).computeSupportResistances(Timeframe.DAILY);
    verify(indicatorCalculator, times(1)).computeIndicators(Timeframe.DAILY);
  }
}
