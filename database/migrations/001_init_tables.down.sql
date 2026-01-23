-- The tables have to be dropped in this specific order
-- because of the referential integrity between the tables
DROP TABLE IF EXISTS renko_data;
DROP TABLE IF EXISTS market_state;
DROP TABLE IF EXISTS market_data;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS tickers;
