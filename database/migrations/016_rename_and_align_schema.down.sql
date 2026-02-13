-- Rollback column names
ALTER TABLE renko_bricks RENAME COLUMN renko_brick_date TO renko_date;
ALTER TABLE renko_bricks RENAME COLUMN renko_brick_id TO renko_data_id;

ALTER TABLE indicators RENAME COLUMN indicator_date TO market_state_date;
ALTER TABLE indicators RENAME COLUMN indicator_id TO market_state_id;

ALTER TABLE data_files RENAME COLUMN data_file_url TO file_url;
ALTER TABLE data_files RENAME COLUMN data_file_date TO file_date;
ALTER TABLE data_files RENAME COLUMN data_file_id TO file_id;

ALTER TABLE candle_bars RENAME COLUMN candle_bar_date TO market_data_date;
ALTER TABLE candle_bars RENAME COLUMN candle_bar_id TO market_data_id;

ALTER TABLE backtest_trades RENAME COLUMN backtest_trades_id TO backtest_trade_id;
ALTER TABLE backtest_signals RENAME COLUMN backtest_signals_id TO backtest_signal_id;
ALTER TABLE backtest_indicators RENAME COLUMN backtest_indicator_id TO backtest_strategy_indicator_id;
ALTER TABLE backtest_equities RENAME COLUMN backtest_equities_id TO backtest_equity_id;

-- Rollback tables
ALTER TABLE renko_bricks RENAME TO renko;
ALTER TABLE candle_bars RENAME TO candles;
