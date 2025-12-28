ALTER TABLE trade_data
DROP COLUMN IF EXISTS volume_profile_poc;

ALTER TABLE trade_data
DROP COLUMN IF EXISTS volume_profile_vah;

ALTER TABLE trade_data
DROP COLUMN IF EXISTS volume_profile_val;
