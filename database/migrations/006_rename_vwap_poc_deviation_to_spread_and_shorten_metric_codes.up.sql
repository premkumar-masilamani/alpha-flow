ALTER TABLE market_data RENAME COLUMN vwap_poc_deviation_pct TO vwap_poc_spread;

-- Update the column type to match prices if it was restricted before
ALTER TABLE market_data ALTER COLUMN vwap_poc_spread TYPE NUMERIC(28, 8);

UPDATE market_state SET metric = 'vol' WHERE metric = 'VOLUME';
UPDATE market_state SET metric = 'vwap' WHERE metric = 'VWAP';
UPDATE market_state SET metric = 'poc' WHERE metric = 'VP_POC';
UPDATE market_state SET metric = 'vpr' WHERE metric = 'VP_VALUE_RANGE';
UPDATE market_state SET metric = 'bcs' WHERE metric = 'BUYER_CAPITAL_SHARE';
UPDATE market_state SET metric = 'bvs' WHERE metric = 'BUYER_VOLUME_SHARE';
