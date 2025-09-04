-- Indexes for performance
CREATE INDEX idx_file_statuses_ticker_date ON file_statuses(ticker, "date");
CREATE INDEX idx_daily_data_ticker_date ON daily_data(ticker, "date");
