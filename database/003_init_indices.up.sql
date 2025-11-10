CREATE INDEX idx_trade_data_ticker_time ON trade_data (ticker_id, trade_time DESC);
CREATE INDEX idx_files_ticker_date ON files (ticker_id, file_date DESC);
