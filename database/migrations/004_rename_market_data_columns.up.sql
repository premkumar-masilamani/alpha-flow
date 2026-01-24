ALTER TABLE market_data RENAME COLUMN volume_profile_poc TO capital_poc;
ALTER TABLE market_data RENAME COLUMN volume_profile_vah TO capital_vah;
ALTER TABLE market_data RENAME COLUMN volume_profile_val TO capital_val;
ALTER TABLE market_data RENAME COLUMN buyer_volume_share TO buyer_capital;
ALTER TABLE market_data RENAME COLUMN buyer_capital_share TO total_capital;

-- Update market_state table metric codes
-- Order matters to avoid collision if any code matches another during transition
UPDATE market_state SET metric = 'T_CAP' WHERE metric = 'B_CAP';
UPDATE market_state SET metric = 'B_CAP' WHERE metric = 'B_VOL';
UPDATE market_state SET metric = 'C_POC' WHERE metric = 'VP_POC';
UPDATE market_state SET metric = 'C_VR' WHERE metric = 'VP_VR';
