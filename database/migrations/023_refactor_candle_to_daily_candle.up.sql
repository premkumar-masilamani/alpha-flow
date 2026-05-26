-- 1. Rename table candle_data (or candle_bars) to daily_candle_data
DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'candle_bars') THEN
            ALTER TABLE candle_bars RENAME TO daily_candle_data;
        ELSIF EXISTS (SELECT FROM pg_tables WHERE tablename = 'candle_data') THEN
            ALTER TABLE candle_data RENAME TO daily_candle_data;
        END IF;
    END
$$;

-- 2. Rename primary key column to daily_candle_data_id
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'daily_candle_data' AND column_name = 'candle_bar_id') THEN
            ALTER TABLE daily_candle_data RENAME COLUMN candle_bar_id TO daily_candle_data_id;
        ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'daily_candle_data' AND column_name = 'candle_data_id') THEN
            ALTER TABLE daily_candle_data RENAME COLUMN candle_data_id TO daily_candle_data_id;
        END IF;
    END
$$;

-- 3. Rename candle_bar_date to candle_date if it is not already candle_date
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'daily_candle_data' AND column_name = 'candle_bar_date') THEN
            ALTER TABLE daily_candle_data RENAME COLUMN candle_bar_date TO candle_date;
        END IF;
    END
$$;

-- 4. Drop the indicator columns from daily_candle_data
ALTER TABLE daily_candle_data DROP COLUMN IF EXISTS vwap;
ALTER TABLE daily_candle_data DROP COLUMN IF EXISTS capital_poc;
ALTER TABLE daily_candle_data DROP COLUMN IF EXISTS capital_vah;
ALTER TABLE daily_candle_data DROP COLUMN IF EXISTS capital_val;
ALTER TABLE daily_candle_data DROP COLUMN IF EXISTS buyer_capital;
ALTER TABLE daily_candle_data DROP COLUMN IF EXISTS total_capital;

-- 5. Drop column is_final from weekly_candle_data
ALTER TABLE weekly_candle_data DROP COLUMN IF EXISTS is_final;

-- 6. Rename foreign key constraint fk_market_data_tickers to fk_daily_candle_data_tickers
ALTER TABLE daily_candle_data RENAME CONSTRAINT fk_market_data_tickers TO fk_daily_candle_data_tickers;

-- 7. Rename unique constraint uk_market_data to uk_daily_candle_data
ALTER TABLE daily_candle_data RENAME CONSTRAINT uk_market_data TO uk_daily_candle_data;

-- 8. Rename index idx_market_data_ticker to idx_daily_candle_data_ticker
ALTER INDEX IF EXISTS idx_market_data_ticker RENAME TO idx_daily_candle_data_ticker;

-- 9. Delete crypto related rows
DELETE FROM tickers WHERE ticker_symbol IN ('BTCUSDT', 'ETHUSDT');
