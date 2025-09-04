-- Drop Unique Constraints
ALTER TABLE trade_data DROP CONSTRAINT uq_trade_data;
ALTER TABLE files DROP CONSTRAINT uq_files;
ALTER TABLE trade_data DROP CONSTRAINT fk_trade_data_interval;
ALTER TABLE trade_data DROP CONSTRAINT fk_trade_data_ticker;
ALTER TABLE files DROP CONSTRAINT fk_files_ticker;
