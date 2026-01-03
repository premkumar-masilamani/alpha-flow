-- Drop Unique Constraints
ALTER TABLE market_state_daily
    DROP CONSTRAINT IF EXISTS uk_market_state_daily;

ALTER TABLE market_data_daily
    DROP CONSTRAINT IF EXISTS uk_market_data_daily;

ALTER TABLE files
    DROP CONSTRAINT IF EXISTS uk_files;
