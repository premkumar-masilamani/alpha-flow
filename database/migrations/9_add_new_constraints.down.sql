-- Drop Unique Constraints
ALTER TABLE file_statuses DROP CONSTRAINT uq_file_statuses_ticker_date_source;
ALTER TABLE trade_data DROP CONSTRAINT uq_trade_data_ticker_date_interval;
