-- Symbol lookup
CREATE UNIQUE INDEX idx_tickers_symbol ON tickers(symbol);

-- Interval lookup
CREATE UNIQUE INDEX idx_intervals_label ON intervals(label);

-- File status queries
CREATE INDEX idx_files_ticker_date ON files(ticker_id, file_date);

-- Trade data queries
CREATE INDEX idx_trade_data_ticker_time ON trade_data(ticker_id, trade_time);
CREATE INDEX idx_trade_data_interval ON trade_data(interval_id);
