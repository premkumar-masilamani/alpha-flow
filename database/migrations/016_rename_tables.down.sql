DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_equities') THEN
            ALTER TABLE backtest_equities
                RENAME TO backtest_equity;
        END IF;
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_signals') THEN
            ALTER TABLE backtest_signals
                RENAME TO backtest_signal;
        END IF;
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_indicators') THEN
            ALTER TABLE backtest_indicators
                RENAME TO backtest_strategy_indicators;
        END IF;
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'backtest_trades') THEN
            ALTER TABLE backtest_trades
                RENAME TO backtest_trade;
        END IF;
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'data_files') THEN
            ALTER TABLE data_files
                RENAME TO files;
        END IF;
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'candles') THEN
            ALTER TABLE candles
                RENAME TO market_data;
        END IF;
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'indicators') THEN
            ALTER TABLE indicators
                RENAME TO market_state;
        END IF;
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'renko') THEN
            ALTER TABLE renko
                RENAME TO renko_data;
        END IF;
    END
$$;
