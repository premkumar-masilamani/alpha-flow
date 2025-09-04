-- Indexes for performance
CREATE INDEX idx_file_statuses_ticker_date ON file_statuses(ticker, "date");
CREATE INDEX idx_trade_data_ticker_date_interval ON trade_data(ticker, "date", interval);
