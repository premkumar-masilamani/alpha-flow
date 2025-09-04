-- Unique Constraints
ALTER TABLE file_statuses ADD CONSTRAINT uq_file_statuses_ticker_date_source UNIQUE (ticker, "date", source);
ALTER TABLE trade_data ADD CONSTRAINT uq_trade_data_ticker_date_interval UNIQUE (ticker, "date", interval);
