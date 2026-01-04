-- Foreign keys
ALTER TABLE files
    ADD CONSTRAINT fk_files_tickers
        FOREIGN KEY (ticker_id)
            REFERENCES tickers (ticker_id)
            ON DELETE CASCADE;

ALTER TABLE market_data
    ADD CONSTRAINT fk_market_data_tickers
        FOREIGN KEY (ticker_id)
            REFERENCES tickers (ticker_id)
            ON DELETE CASCADE;

ALTER TABLE market_state
    ADD CONSTRAINT fk_market_state_tickers
        FOREIGN KEY (ticker_id)
            REFERENCES tickers (ticker_id)
            ON DELETE CASCADE;


-- Unique constraints
ALTER TABLE files
    ADD CONSTRAINT uk_files UNIQUE (ticker_id, file_date);

ALTER TABLE market_data
    ADD CONSTRAINT uk_market_data UNIQUE (ticker_id, market_data_date);

ALTER TABLE market_state
    ADD CONSTRAINT uk_market_state UNIQUE (ticker_id, market_state_date, metric, ma_type, period);
