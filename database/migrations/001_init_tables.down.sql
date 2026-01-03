-- The tables has to be dropped in this specific order
-- because of the referential integrity between the tables
DROP TABLE market_state_daily;
DROP TABLE market_data_daily;
DROP TABLE files;
DROP TABLE tickers;
