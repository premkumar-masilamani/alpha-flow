-- Drop Unique Constraints
ALTER TABLE trade_data DROP CONSTRAINT uk_trade_data_ticker_id_trade_date;
ALTER TABLE files DROP CONSTRAINT uk_files_ticker_id_file_date;
