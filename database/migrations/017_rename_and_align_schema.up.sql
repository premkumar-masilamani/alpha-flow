-- Rename tables
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'candles') THEN
        ALTER TABLE candles RENAME TO candle_bars;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'renko') THEN
        ALTER TABLE renko RENAME TO renko_bricks;
    END IF;
END $$;

-- Align column names with Java Entities
DO $$
BEGIN
    -- backtest_equities
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_equities' AND column_name='backtest_equity_id') THEN
        ALTER TABLE backtest_equities RENAME COLUMN backtest_equity_id TO backtest_equities_id;
    END IF;

    -- backtest_indicators
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_indicators' AND column_name='backtest_strategy_indicator_id') THEN
        ALTER TABLE backtest_indicators RENAME COLUMN backtest_strategy_indicator_id TO backtest_indicator_id;
    END IF;

    -- backtest_signals
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_signals' AND column_name='backtest_signal_id') THEN
        ALTER TABLE backtest_signals RENAME COLUMN backtest_signal_id TO backtest_signals_id;
    END IF;

    -- backtest_trades
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_trades' AND column_name='backtest_trade_id') THEN
        ALTER TABLE backtest_trades RENAME COLUMN backtest_trade_id TO backtest_trades_id;
    END IF;

    -- candle_bars
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='candle_bars' AND column_name='market_data_id') THEN
        ALTER TABLE candle_bars RENAME COLUMN market_data_id TO candle_bar_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='candle_bars' AND column_name='market_data_date') THEN
        ALTER TABLE candle_bars RENAME COLUMN market_data_date TO candle_bar_date;
    END IF;

    -- data_files
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='data_files' AND column_name='file_id') THEN
        ALTER TABLE data_files RENAME COLUMN file_id TO data_file_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='data_files' AND column_name='file_date') THEN
        ALTER TABLE data_files RENAME COLUMN file_date TO data_file_date;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='data_files' AND column_name='file_url') THEN
        ALTER TABLE data_files RENAME COLUMN file_url TO data_file_url;
    END IF;

    -- indicators
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicators' AND column_name='market_state_id') THEN
        ALTER TABLE indicators RENAME COLUMN market_state_id TO indicator_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicators' AND column_name='market_state_date') THEN
        ALTER TABLE indicators RENAME COLUMN market_state_date TO indicator_date;
    END IF;

    -- renko_bricks
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='renko_bricks' AND column_name='renko_data_id') THEN
        ALTER TABLE renko_bricks RENAME COLUMN renko_data_id TO renko_brick_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='renko_bricks' AND column_name='renko_date') THEN
        ALTER TABLE renko_bricks RENAME COLUMN renko_date TO renko_brick_date;
    END IF;
END $$;
