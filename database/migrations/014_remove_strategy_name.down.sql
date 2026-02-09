-- Migration to add strategy_name column back to backtest tables
ALTER TABLE backtest_results
    ADD COLUMN strategy_name VARCHAR(255);
ALTER TABLE backtest_equity
    ADD COLUMN strategy_name VARCHAR(255);
ALTER TABLE backtest_signal
    ADD COLUMN strategy_name VARCHAR(255);
ALTER TABLE backtest_trade
    ADD COLUMN strategy_name VARCHAR(255);
