ALTER TABLE tickers DROP COLUMN source;

-- Re-applying NOT NULL might fail if there are NULL values, but this is the logical reverse
ALTER TABLE market_data ALTER COLUMN vwap SET NOT NULL;
ALTER TABLE market_data ALTER COLUMN capital_poc SET NOT NULL;
ALTER TABLE market_data ALTER COLUMN capital_vah SET NOT NULL;
ALTER TABLE market_data ALTER COLUMN capital_val SET NOT NULL;
ALTER TABLE market_data ALTER COLUMN buyer_capital SET NOT NULL;
ALTER TABLE market_data ALTER COLUMN total_capital SET NOT NULL;
