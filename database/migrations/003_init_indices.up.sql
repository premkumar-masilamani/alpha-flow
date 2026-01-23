CREATE INDEX idx_files_unprocessed
    ON files (file_id)
    WHERE is_processed = false;

CREATE INDEX idx_market_data_ticker ON market_data (ticker_id);

CREATE INDEX idx_market_state_ticker ON market_state (ticker_id);

CREATE INDEX idx_market_state_metric_lookup
    ON market_state (ticker_id, metric, ma_type, period, market_state_date);

CREATE INDEX idx_renko_data_ticker ON renko_data (ticker_id);