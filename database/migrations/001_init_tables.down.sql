-- The tables have to be dropped in this specific order
-- because of the referential integrity between the tables
DROP TABLE IF EXISTS market_state_daily;
DROP TABLE IF EXISTS market_data_daily;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS tickers;
