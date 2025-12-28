ALTER TABLE trade_data
RENAME COLUMN buyer_volume_ratio TO buyer_volume_share;

ALTER TABLE trade_data
RENAME COLUMN buyer_capital_ratio TO buyer_capital_share;
