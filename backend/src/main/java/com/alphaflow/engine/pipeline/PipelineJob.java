package com.alphaflow.engine.pipeline;

import com.alphaflow.common.enums.Timeframe;

/**
 * Common lifecycle contract for batch pipeline jobs and technical analysis calculators.
 */
public interface PipelineJob {

  /**
   * Returns the unique identifier for this pipeline job or step
   * (e.g. "DOWNLOAD_DAILY_PRICE", "CALCULATE_INDICATOR").
   *
   * @return unique job name
   */
  String getJobName();

  /**
   * Queries the database, compares price dates against today's date for each active ticker's
   * country timezone (America/New_York for US, Asia/Kolkata for IN), and decides if the job should run.
   *
   * @param timeframe the market timeframe (DAILY / WEEKLY), or null for single-timeframe jobs
   * @return true if at least one active ticker is missing data or needs updating; false otherwise
   */
  boolean shouldRun(Timeframe timeframe);

  /**
   * Executes the pipeline job logic for the specified timeframe.
   *
   * @param timeframe the market timeframe (DAILY / WEEKLY), or null for single-timeframe jobs
   */
  void execute(Timeframe timeframe);
}
