-- The tables has to be dropped in this specific order
-- because of the referential integrity between the tables
DROP TABLE trade_data;
DROP TABLE files;
DROP TABLE intervals;
DROP TABLE tickers;
