-- Migration to remove strategy_name column from backtest tables
ALTER TABLE backtest_results DROP COLUMN strategy_name;
ALTER TABLE backtest_equity DROP COLUMN strategy_name;
ALTER TABLE backtest_signal DROP COLUMN strategy_name;
ALTER TABLE backtest_trade DROP COLUMN strategy_name;
