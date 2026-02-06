ALTER TABLE backtest_trade DROP CONSTRAINT IF EXISTS fk_trade_strategy;
ALTER TABLE backtest_trade DROP COLUMN IF EXISTS backtest_strategy_id;

ALTER TABLE backtest_signal DROP CONSTRAINT IF EXISTS fk_signal_strategy;
ALTER TABLE backtest_signal DROP COLUMN IF EXISTS backtest_strategy_id;

ALTER TABLE backtest_equity DROP CONSTRAINT IF EXISTS fk_equity_strategy;
ALTER TABLE backtest_equity DROP COLUMN IF EXISTS backtest_strategy_id;

ALTER TABLE backtest_results DROP CONSTRAINT IF EXISTS fk_results_strategy;
ALTER TABLE backtest_results DROP COLUMN IF EXISTS backtest_strategy_id;

DROP TABLE IF EXISTS backtest_strategy_indicators;
DROP TABLE IF EXISTS backtest_strategies;
