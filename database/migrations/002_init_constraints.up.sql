-- Foreign keys
ALTER TABLE files
  ADD CONSTRAINT fk_files_tickers FOREIGN KEY (ticker_id)
    REFERENCES tickers(ticker_id) ON DELETE CASCADE;

ALTER TABLE trade_data
    ADD CONSTRAINT fk_trade_data_tickers FOREIGN KEY (ticker_id)
    REFERENCES tickers(ticker_id) ON DELETE CASCADE;

-- Unique constraints
ALTER TABLE files
    ADD CONSTRAINT uk_files_ticker_id_file_date UNIQUE (ticker_id, file_date);

ALTER TABLE trade_data
    ADD CONSTRAINT uk_trade_data_ticker_id_trade_date UNIQUE (ticker_id, trade_date);
