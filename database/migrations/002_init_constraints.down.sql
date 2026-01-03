-- Drop Unique Constraints
ALTER TABLE market_state
    DROP CONSTRAINT IF EXISTS uk_market_state;

ALTER TABLE market_data
    DROP CONSTRAINT IF EXISTS uk_market_data;

ALTER TABLE files
    DROP CONSTRAINT IF EXISTS uk_files;
