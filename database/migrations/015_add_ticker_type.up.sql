-- Migration to add ticker_type column and seed new tickers
ALTER TABLE tickers ADD COLUMN ticker_type VARCHAR(50);

-- Mark existing Binance tickers as CRYPTO
UPDATE tickers SET ticker_type = 'CRYPTO';

-- Add new tickers from Yahoo Finance
-- Gold and Silver (COMMODITY)
INSERT INTO tickers (ticker_date, ticker_symbol, ticker_name, source, ticker_type, is_active)
VALUES ('1950-01-01', 'GC=F', 'Gold', 'YAHOO_FINANCE', 'COMMODITY', true),
       ('1950-01-01', 'SI=F', 'Silver', 'YAHOO_FINANCE', 'COMMODITY', true);

-- Yahoo Crypto (CRYPTO)
INSERT INTO tickers (ticker_date, ticker_symbol, ticker_name, source, ticker_type, is_active)
VALUES ('1950-01-01', 'BTC-USD', 'Bitcoin', 'YAHOO_FINANCE', 'CRYPTO', true),
       ('1950-01-01', 'ETH-USD', 'Ethereum', 'YAHOO_FINANCE', 'CRYPTO', true),
       ('1950-01-01', 'XRP-USD', 'Ripple', 'YAHOO_FINANCE', 'CRYPTO', true),
       ('1950-01-01', 'SOL-USD', 'Solana', 'YAHOO_FINANCE', 'CRYPTO', true),
       ('1950-01-01', 'LINK-USD', 'Chainlink', 'YAHOO_FINANCE', 'CRYPTO', true),
       ('1950-01-01', 'DOGE-USD', 'Dogecoin', 'YAHOO_FINANCE', 'CRYPTO', true);
