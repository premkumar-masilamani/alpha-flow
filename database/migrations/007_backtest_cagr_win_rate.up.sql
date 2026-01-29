-- Migration to add win_rate and remove pnl_pct from backtest_cagr table

ALTER TABLE backtest_cagr ADD COLUMN win_rate NUMERIC(28, 8);
ALTER TABLE backtest_cagr DROP COLUMN IF EXISTS pnl_pct;
