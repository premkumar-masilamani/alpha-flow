INSERT INTO tickers (ticker_symbol, ticker_name, ticker_type, country, is_active, data_provider)
VALUES ('BRK-B', 'Berkshire Hathaway', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('LLY', 'Eli Lilly', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('AVGO', 'Broadcom', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('AAPL', 'Apple Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('MSFT', 'Microsoft Corporation', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('GOOGL', 'Alphabet Inc. (Class A)', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('AMZN', 'Amazon.com, Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('META', 'Meta Platforms, Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('NVDA', 'NVIDIA Corporation', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('TSLA', 'Tesla, Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE'),
       ('^GSPC', 'S&P 500', 'INDEX', 'US', true, 'YAHOO_FINANCE'),
       ('^DJI', 'Dow Jones Industrial Average', 'INDEX', 'US', true, 'YAHOO_FINANCE'),
       ('^IXIC', 'NASDAQ Composite', 'INDEX', 'US', true, 'YAHOO_FINANCE'),
       ('NIFTY50', 'NIFTY 50', 'INDEX', 'IN', true, 'ANGEL_ONE')
ON CONFLICT (ticker_symbol) DO NOTHING;

INSERT INTO indicator_definitions (indicator_type, source, params, bounds)
VALUES ('EMA', 'CLOSE', '{"period": 5}', NULL),
       ('EMA', 'CLOSE', '{"period": 13}', NULL),
       ('EMA', 'CLOSE', '{"period": 26}', NULL),
       ('RSI', 'CLOSE', '{"period": 14}', '{"upperBound": 70, "lowerBound": 30}'),
       ('STOCHASTIC', 'CLOSE', '{"k": 14, "kSmooth": 3, "dSmooth": 3}', '{"upperBound": 80, "lowerBound": 20}'),
       ('SMA', 'VOLUME', '{"period": 20}', NULL),
       ('MACD', 'CLOSE', '{"fast": 12, "slow": 26, "signal": 9}', NULL),
       ('BB', 'CLOSE', '{"period": 20, "stdDev": 2}', NULL)
ON CONFLICT (indicator_type, source, params) DO NOTHING;

INSERT INTO market_holidays (country, holiday_date, name)
VALUES
       ('US', '2026-01-01', 'New Year’s Day'),
       ('US', '2026-01-19', 'Martin Luther King, Jr. Day'),
       ('US', '2026-02-16', 'Washington''s Birthday'),
       ('US', '2026-04-03', 'Good Friday'),
       ('US', '2026-05-25', 'Memorial Day'),
       ('US', '2026-06-19', 'Juneteenth National Independence Day'),
       ('US', '2026-07-03', 'Independence Day (observed)'),
       ('US', '2026-09-07', 'Labor Day'),
       ('US', '2026-11-26', 'Thanksgiving Day'),
       ('US', '2026-12-25', 'Christmas Day'),
       ('IN', '2026-01-15', 'Municipal Corporation Election - Maharashtra'),
       ('IN', '2026-01-26', 'Republic Day'),
       ('IN', '2026-03-03', 'Holi'),
       ('IN', '2026-03-26', 'Shri Ram Navami'),
       ('IN', '2026-03-31', 'Shri Mahavir Jayanti'),
       ('IN', '2026-04-03', 'Good Friday'),
       ('IN', '2026-04-14', 'Dr. Baba Saheb Ambedkar Jayanti'),
       ('IN', '2026-05-01', 'Maharashtra Day'),
       ('IN', '2026-05-28', 'Bakri Id'),
       ('IN', '2026-06-26', 'Muharram'),
       ('IN', '2026-09-14', 'Ganesh Chaturthi'),
       ('IN', '2026-10-02', 'Mahatma Gandhi Jayanti'),
       ('IN', '2026-10-20', 'Dussehra'),
       ('IN', '2026-11-10', 'Diwali-Balipratipada'),
       ('IN', '2026-11-24', 'Prakash Gurpurb Sri Guru Nanak Dev'),
       ('IN', '2026-12-25', 'Christmas')
ON CONFLICT (country, holiday_date) DO NOTHING;
