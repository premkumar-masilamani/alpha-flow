-- Drop Unique Constraints
ALTER TABLE file_statuses DROP CONSTRAINT uq_file_statuses_ticker_date_source;
ALTER TABLE daily_data DROP CONSTRAINT uq_daily_data_ticker_date;
