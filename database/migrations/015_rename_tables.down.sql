ALTER TABLE backtest_equities RENAME TO backtest_equity;
ALTER TABLE backtest_signals RENAME TO backtest_signal;
ALTER TABLE backtest_indicators RENAME TO backtest_strategy_indicators;
ALTER TABLE backtest_trades RENAME TO backtest_trade;
ALTER TABLE data_files RENAME TO files;
ALTER TABLE candles RENAME TO market_data;
ALTER TABLE indicators RENAME TO market_state;
ALTER TABLE renko RENAME TO renko_data;
