-- Migration to add win_rate and remove pnl_pct from backtest_cagr table
-- Also rename backtest_cagr table to backtest_results

ALTER TABLE backtest_cagr ADD COLUMN win_rate NUMERIC(28, 8);
ALTER TABLE backtest_cagr DROP COLUMN IF EXISTS pnl_pct;
ALTER TABLE backtest_cagr RENAME TO backtest_results;
ALTER TABLE backtest_results RENAME COLUMN backtest_cagr_id TO backtest_result_id;
