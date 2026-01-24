ALTER TABLE market_data RENAME COLUMN capital_poc TO volume_profile_poc;
ALTER TABLE market_data RENAME COLUMN capital_vah TO volume_profile_vah;
ALTER TABLE market_data RENAME COLUMN capital_val TO volume_profile_val;
ALTER TABLE market_data RENAME COLUMN buyer_capital TO buyer_volume_share;
ALTER TABLE market_data RENAME COLUMN total_capital TO buyer_capital_share;

-- Revert market_state table metric codes
UPDATE market_state SET metric = 'B_VOL' WHERE metric = 'B_CAP';
UPDATE market_state SET metric = 'B_CAP' WHERE metric = 'T_CAP';
UPDATE market_state SET metric = 'VP_POC' WHERE metric = 'C_POC';
UPDATE market_state SET metric = 'VP_VR' WHERE metric = 'C_VR';
