ALTER TABLE trade_data
RENAME COLUMN buyer_volume_share TO buyer_volume_ratio;

ALTER TABLE trade_data
RENAME COLUMN buyer_capital_share TO buyer_capital_ratio;
