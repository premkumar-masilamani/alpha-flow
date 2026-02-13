-- Rename tables
ALTER TABLE candles RENAME TO candle_bars;
ALTER TABLE renko RENAME TO renko_bricks;

-- Align column names with Java Entities
ALTER TABLE backtest_equities RENAME COLUMN backtest_equity_id TO backtest_equities_id;
ALTER TABLE backtest_indicators RENAME COLUMN backtest_strategy_indicator_id TO backtest_indicator_id;
ALTER TABLE backtest_signals RENAME COLUMN backtest_signal_id TO backtest_signals_id;
ALTER TABLE backtest_trades RENAME COLUMN backtest_trade_id TO backtest_trades_id;

ALTER TABLE candle_bars RENAME COLUMN market_data_id TO candle_bar_id;
ALTER TABLE candle_bars RENAME COLUMN market_data_date TO candle_bar_date;

ALTER TABLE data_files RENAME COLUMN file_id TO data_file_id;
ALTER TABLE data_files RENAME COLUMN file_date TO data_file_date;
ALTER TABLE data_files RENAME COLUMN file_url TO data_file_url;

ALTER TABLE indicators RENAME COLUMN market_state_id TO indicator_id;
ALTER TABLE indicators RENAME COLUMN market_state_date TO indicator_date;

ALTER TABLE renko_bricks RENAME COLUMN renko_data_id TO renko_brick_id;
ALTER TABLE renko_bricks RENAME COLUMN renko_date TO renko_brick_date;
