ALTER TABLE backtest_signal DROP COLUMN signal_data;
ALTER TABLE backtest_signal ADD COLUMN from_position VARCHAR(50);
ALTER TABLE backtest_signal ADD COLUMN to_position VARCHAR(50);
