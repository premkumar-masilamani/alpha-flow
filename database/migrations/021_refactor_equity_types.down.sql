-- 1. Revert 'US-EQUITY' ticker_type to 'STOCKS'
UPDATE tickers
SET ticker_type = 'STOCKS'
WHERE ticker_type = 'US-EQUITY';

-- 2. Delete IN-EQUITY tickers
DELETE FROM tickers WHERE ticker_type = 'IN-EQUITY';

-- 3. Reset LLY, AVGO, BRK-B ticker_type
UPDATE tickers
SET ticker_type = 'STOCKS'
WHERE ticker_symbol IN ('BRK-B', 'LLY', 'AVGO');
