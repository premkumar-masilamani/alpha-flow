CREATE INDEX idx_files_unprocessed
    ON files (file_id)
    WHERE is_processed = false;

CREATE INDEX idx_market_state_metric_lookup
    ON market_state (ticker_id, metric, ma_type, period, market_state_date);
