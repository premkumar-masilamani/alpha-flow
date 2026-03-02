-- Migration to add strategy_state column to backtest_signals table
ALTER TABLE backtest_signals ADD COLUMN strategy_state TEXT;
