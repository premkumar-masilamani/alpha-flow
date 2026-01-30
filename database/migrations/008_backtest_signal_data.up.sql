ALTER TABLE backtest_signal DROP COLUMN from_position;
ALTER TABLE backtest_signal DROP COLUMN to_position;
ALTER TABLE backtest_signal ADD COLUMN signal_data TEXT;
