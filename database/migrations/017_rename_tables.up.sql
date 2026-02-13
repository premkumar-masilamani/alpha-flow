DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_equity') THEN
        ALTER TABLE backtest_equity RENAME TO backtest_equities;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_signal') THEN
        ALTER TABLE backtest_signal RENAME TO backtest_signals;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_strategy_indicators') THEN
        ALTER TABLE backtest_strategy_indicators RENAME TO backtest_indicators;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_trade') THEN
        ALTER TABLE backtest_trade RENAME TO backtest_trades;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'files') THEN
        ALTER TABLE files RENAME TO data_files;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'market_data') THEN
        ALTER TABLE market_data RENAME TO candles;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'market_state') THEN
        ALTER TABLE market_state RENAME TO indicators;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'renko_data') THEN
        ALTER TABLE renko_data RENAME TO renko;
    END IF;
END $$;
