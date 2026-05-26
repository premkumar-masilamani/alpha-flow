-- 1. Re-add dropped columns to daily_candle_data
ALTER TABLE daily_candle_data ADD COLUMN IF NOT EXISTS vwap NUMERIC(38, 2);
ALTER TABLE daily_candle_data ADD COLUMN IF NOT EXISTS capital_poc NUMERIC(38, 2);
ALTER TABLE daily_candle_data ADD COLUMN IF NOT EXISTS capital_vah NUMERIC(38, 2);
ALTER TABLE daily_candle_data ADD COLUMN IF NOT EXISTS capital_val NUMERIC(38, 2);
ALTER TABLE daily_candle_data ADD COLUMN IF NOT EXISTS buyer_capital NUMERIC(38, 2);
ALTER TABLE daily_candle_data ADD COLUMN IF NOT EXISTS total_capital NUMERIC(38, 2);

-- 2. Re-add is_final to weekly_candle_data
ALTER TABLE weekly_candle_data ADD COLUMN IF NOT EXISTS is_final BOOLEAN DEFAULT TRUE NOT NULL;

-- 3. Rename constraints back
ALTER TABLE daily_candle_data RENAME CONSTRAINT fk_daily_candle_data_tickers TO fk_market_data_tickers;
ALTER TABLE daily_candle_data RENAME CONSTRAINT uk_daily_candle_data TO uk_market_data;

-- 4. Rename index back
ALTER INDEX IF EXISTS idx_daily_candle_data_ticker RENAME TO idx_market_data_ticker;

-- 5. Rename primary key column back to candle_data_id
ALTER TABLE daily_candle_data RENAME COLUMN daily_candle_data_id TO candle_data_id;

-- 6. Rename table back to candle_data
ALTER TABLE daily_candle_data RENAME TO candle_data;

-- 7. Re-insert crypto tickers
INSERT INTO tickers (ticker_id, ticker_date, ticker_symbol, ticker_name, is_active)
VALUES (1, '2017-08-17', 'BTCUSDT', 'Bitcoin (BTC) / ETF (BITB)', true),
       (2, '2017-08-17', 'ETHUSDT', 'Ethereum (ETH) / ETF (ETHW)', false)
ON CONFLICT (ticker_symbol) DO NOTHING;
