ALTER TABLE tickers
    ADD COLUMN source VARCHAR(50) DEFAULT 'BINANCE' NOT NULL;

ALTER TABLE market_data
    ALTER COLUMN vwap DROP NOT NULL;
ALTER TABLE market_data
    ALTER COLUMN capital_poc DROP NOT NULL;
ALTER TABLE market_data
    ALTER COLUMN capital_vah DROP NOT NULL;
ALTER TABLE market_data
    ALTER COLUMN capital_val DROP NOT NULL;
ALTER TABLE market_data
    ALTER COLUMN buyer_capital DROP NOT NULL;
ALTER TABLE market_data
    ALTER COLUMN total_capital DROP NOT NULL;
