ALTER TABLE trade_data
ADD COLUMN volume_profile_poc    NUMERIC(18, 8);

ALTER TABLE trade_data
ADD COLUMN volume_profile_vah    NUMERIC(18, 8);

ALTER TABLE trade_data
ADD COLUMN volume_profile_val    NUMERIC(18, 8);
