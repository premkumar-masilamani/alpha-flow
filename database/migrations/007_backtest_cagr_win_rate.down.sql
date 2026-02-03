-- Migration to rollback backtest_cagr changes

ALTER TABLE backtest_results
    RENAME COLUMN backtest_result_id TO backtest_cagr_id;
ALTER TABLE backtest_results
    RENAME TO backtest_cagr;
ALTER TABLE backtest_cagr
    DROP COLUMN IF EXISTS win_rate;
-- Note: we don't restore pnl_pct as it was not present in the initial 006 migration of this table.
