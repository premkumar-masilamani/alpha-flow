-- Unique Constraints
ALTER TABLE file_statuses ADD CONSTRAINT uq_file_statuses_ticker_date_source UNIQUE (ticker, "date", source);
ALTER TABLE daily_data ADD CONSTRAINT uq_daily_data_ticker_date UNIQUE (ticker, "date");
