UPDATE market_state SET metric = 'VOLUME' WHERE metric = 'vol';
UPDATE market_state SET metric = 'VWAP' WHERE metric = 'vwap';
UPDATE market_state SET metric = 'VP_POC' WHERE metric = 'poc';
UPDATE market_state SET metric = 'VP_VALUE_RANGE' WHERE metric = 'vpr';
UPDATE market_state SET metric = 'BUYER_CAPITAL_SHARE' WHERE metric = 'bcs';
UPDATE market_state SET metric = 'BUYER_VOLUME_SHARE' WHERE metric = 'bvs';

ALTER TABLE market_data ALTER COLUMN vwap_poc_spread TYPE NUMERIC(5, 2);
ALTER TABLE market_data RENAME COLUMN vwap_poc_spread TO vwap_poc_deviation_pct;
