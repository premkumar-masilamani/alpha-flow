-- Rollback column names
DO $$
BEGIN
    -- backtest_equities
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_equities' AND column_name='backtest_equities_id') THEN
        ALTER TABLE backtest_equities RENAME COLUMN backtest_equities_id TO backtest_equity_id;
    END IF;

    -- backtest_indicators
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_indicators' AND column_name='backtest_indicator_id') THEN
        ALTER TABLE backtest_indicators RENAME COLUMN backtest_indicator_id TO backtest_strategy_indicator_id;
    END IF;

    -- backtest_signals
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_signals' AND column_name='backtest_signals_id') THEN
        ALTER TABLE backtest_signals RENAME COLUMN backtest_signals_id TO backtest_signal_id;
    END IF;

    -- backtest_trades
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='backtest_trades' AND column_name='backtest_trades_id') THEN
        ALTER TABLE backtest_trades RENAME COLUMN backtest_trades_id TO backtest_trade_id;
    END IF;

    -- candle_bars
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='candle_bars' AND column_name='candle_bar_id') THEN
        ALTER TABLE candle_bars RENAME COLUMN candle_bar_id TO market_data_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='candle_bars' AND column_name='candle_bar_date') THEN
        ALTER TABLE candle_bars RENAME COLUMN candle_bar_date TO market_data_date;
    END IF;

    -- data_files
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='data_files' AND column_name='data_file_id') THEN
        ALTER TABLE data_files RENAME COLUMN data_file_id TO file_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='data_files' AND column_name='data_file_date') THEN
        ALTER TABLE data_files RENAME COLUMN data_file_date TO file_date;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='data_files' AND column_name='data_file_url') THEN
        ALTER TABLE data_files RENAME COLUMN data_file_url TO file_url;
    END IF;

    -- indicators
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicators' AND column_name='indicator_id') THEN
        ALTER TABLE indicators RENAME COLUMN indicator_id TO market_state_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicators' AND column_name='indicator_date') THEN
        ALTER TABLE indicators RENAME COLUMN indicator_date TO market_state_date;
    END IF;

    -- renko_bricks
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='renko_bricks' AND column_name='renko_brick_id') THEN
        ALTER TABLE renko_bricks RENAME COLUMN renko_brick_id TO renko_data_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='renko_bricks' AND column_name='renko_brick_date') THEN
        ALTER TABLE renko_bricks RENAME COLUMN renko_brick_date TO renko_date;
    END IF;
END $$;

-- Rollback tables
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'candle_bars') THEN
        ALTER TABLE candle_bars RENAME TO candles;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'renko_bricks') THEN
        ALTER TABLE renko_bricks RENAME TO renko;
    END IF;
END $$;
