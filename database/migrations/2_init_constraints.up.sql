-- Primary key must include partition key (trade_time)
ALTER TABLE trade_data
    ADD CONSTRAINT pk_trade_data PRIMARY KEY (trade_time, ticker_id, interval_id);

-- Foreign keys
ALTER TABLE files
  ADD CONSTRAINT fk_files_ticker FOREIGN KEY (ticker_id)
    REFERENCES tickers(ticker_id);

ALTER TABLE trade_data
    ADD CONSTRAINT fk_trade_data_ticker FOREIGN KEY (ticker_id)
    REFERENCES tickers(ticker_id);

ALTER TABLE trade_data
    ADD CONSTRAINT fk_trade_data_interval FOREIGN KEY (interval_id)
    REFERENCES intervals(interval_id);

-- Uniqueness
ALTER TABLE files
  ADD CONSTRAINT uq_files UNIQUE (ticker_id, file_date, source);
