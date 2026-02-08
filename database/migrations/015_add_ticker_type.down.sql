-- Reverse migration for ticker_type and new tickers
DELETE FROM tickers WHERE ticker_symbol IN ('GC=F', 'SI=F', 'BTC-USD', 'ETH-USD', 'XRP-USD', 'SOL-USD', 'LINK-USD', 'DOGE-USD');
ALTER TABLE tickers DROP COLUMN ticker_type;
