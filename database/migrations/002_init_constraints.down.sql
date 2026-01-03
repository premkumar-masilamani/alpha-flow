-- Drop Unique Constraints
ALTER TABLE market_state_daily DROP CONSTRAINT uk_market_state_daily;
ALTER TABLE market_data_daily DROP CONSTRAINT uk_market_data_daily;
ALTER TABLE files DROP CONSTRAINT uk_files;
