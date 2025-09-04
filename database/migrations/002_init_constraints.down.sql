-- Drop Unique Constraints
ALTER TABLE files DROP CONSTRAINT uq_files;
ALTER TABLE trade_data DROP CONSTRAINT fk_trade_data_interval;
ALTER TABLE trade_data DROP CONSTRAINT fk_trade_data_ticker;
ALTER TABLE files DROP CONSTRAINT fk_files_ticker;
ALTER TABLE trade_data DROP CONSTRAINT pk_trade_data;
