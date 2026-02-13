-- Migration to add ticker_type column and seed new tickers
ALTER TABLE tickers
    ADD COLUMN ticker_type VARCHAR(50);
