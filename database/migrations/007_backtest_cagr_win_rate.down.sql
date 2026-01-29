-- Migration to rollback backtest_cagr changes

ALTER TABLE backtest_cagr DROP COLUMN IF EXISTS win_rate;
-- Note: we don't restore pnl_pct as it was not present in the initial 006 migration of this table.
