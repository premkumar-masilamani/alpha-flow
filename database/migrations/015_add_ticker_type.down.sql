-- Reverse migration for ticker_type and new tickers
ALTER TABLE tickers
    DROP COLUMN ticker_type;
