ALTER TABLE market_data
    ADD COLUMN vwap_ohlc4 NUMERIC(28, 8);
ALTER TABLE market_data
    ADD COLUMN vwap_hlc3 NUMERIC(28, 8);