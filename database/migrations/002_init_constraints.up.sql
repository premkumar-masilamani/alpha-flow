-- Primary keys
ALTER TABLE files
    ADD CONSTRAINT pk_files PRIMARY KEY (ticker_id, file_date);

ALTER TABLE trade_data
    ADD CONSTRAINT pk_trade_data PRIMARY KEY (trade_time, ticker_id);

-- Foreign keys
ALTER TABLE files
  ADD CONSTRAINT fk_files_tickers FOREIGN KEY (ticker_id)
    REFERENCES tickers(ticker_id) ON DELETE CASCADE;

ALTER TABLE trade_data
    ADD CONSTRAINT fk_trade_data_tickers FOREIGN KEY (ticker_id)
    REFERENCES tickers(ticker_id) ON DELETE CASCADE;
