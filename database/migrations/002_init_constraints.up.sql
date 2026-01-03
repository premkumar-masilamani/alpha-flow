-- Foreign keys
ALTER TABLE files
    ADD CONSTRAINT fk_files FOREIGN KEY (ticker_id)
        REFERENCES tickers (ticker_id) ON DELETE CASCADE;

ALTER TABLE market_data_daily
    ADD CONSTRAINT fk_market_data_daily FOREIGN KEY (ticker_id)
        REFERENCES tickers (ticker_id) ON DELETE CASCADE;

ALTER TABLE market_state_daily
    ADD CONSTRAINT fk_market_state_daily FOREIGN KEY (ticker_id)
        REFERENCES tickers (ticker_id) ON DELETE CASCADE;

-- Unique constraints
ALTER TABLE files
    ADD CONSTRAINT uk_files UNIQUE (file_date, ticker_id);

ALTER TABLE market_data_daily
    ADD CONSTRAINT uk_market_data_daily UNIQUE (market_data_date, ticker_id);

ALTER TABLE market_state_daily
    ADD CONSTRAINT uk_market_state_daily UNIQUE (market_state_date, ticker_id, metric, ma_type, period);
