-- Drop Unique Constraints
ALTER TABLE trade_data DROP CONSTRAINT fk_trade_data_tickers;
ALTER TABLE files DROP CONSTRAINT fk_files_tickers;
ALTER TABLE trade_data DROP CONSTRAINT pk_trade_data;
ALTER TABLE files DROP CONSTRAINT pk_files;
