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
       ('NIFTY50', 'NIFTY 50', 'INDEX', 'IN', true, 'ANGEL_ONE');

INSERT INTO public.indicator_definitions (indicator_type, source, params)
VALUES ('EMA', 'CLOSE', '{"period": 5}'),
       ('EMA', 'CLOSE', '{"period": 13}'),
       ('EMA', 'CLOSE', '{"period": 26}'),
       ('RSI', 'CLOSE', '{"period": 14}'),
       ('STOCHASTIC', 'CLOSE', '{"k": 14, "kSmooth": 3, "dSmooth": 3}'),
       ('SMA', 'VOLUME', '{"period": 20}'),
       ('MACD', 'CLOSE', '{"fast": 12, "slow": 26, "signal": 9}'),
       ('BB', 'CLOSE', '{"period": 20, "stdDev": 2}');

INSERT INTO market_holidays (country, holiday_date, name, description)
VALUES -- US Market 2026
       ('US', '2026-01-01', 'New Year’s Day', NULL),
       ('US', '2026-01-19', 'Martin Luther King, Jr. Day', NULL),
       ('US', '2026-02-16', 'Washington''s Birthday', NULL),
       ('US', '2026-04-03', 'Good Friday', NULL),
       ('US', '2026-05-25', 'Memorial Day', NULL),
       ('US', '2026-06-19', 'Juneteenth National Independence Day', NULL),
       ('US', '2026-07-03', 'Independence Day (observed)', NULL),
       ('US', '2026-09-07', 'Labor Day', NULL),
       ('US', '2026-11-26', 'Thanksgiving Day', NULL),
       ('US', '2026-12-25', 'Christmas Day', NULL),

       -- US Market 2027
       ('US', '2027-01-01', 'New Year’s Day', NULL),
       ('US', '2027-01-18', 'Martin Luther King, Jr. Day', NULL),
       ('US', '2027-02-15', 'Washington''s Birthday', NULL),
       ('US', '2027-03-26', 'Good Friday', NULL),
       ('US', '2027-05-31', 'Memorial Day', NULL),
       ('US', '2027-06-18', 'Juneteenth National Independence Day (observed)', NULL),
       ('US', '2027-07-05', 'Independence Day (observed)', NULL),
       ('US', '2027-09-06', 'Labor Day', NULL),
       ('US', '2027-11-25', 'Thanksgiving Day', NULL),
       ('US', '2027-12-24', 'Christmas Day (observed)', NULL),

       -- US Market 2028
       ('US', '2028-01-17', 'Martin Luther King, Jr. Day', NULL),
       ('US', '2028-02-21', 'Washington''s Birthday', NULL),
       ('US', '2028-04-14', 'Good Friday', NULL),
       ('US', '2028-05-29', 'Memorial Day', NULL),
       ('US', '2028-06-19', 'Juneteenth National Independence Day', NULL),
       ('US', '2028-07-04', 'Independence Day', NULL),
       ('US', '2028-09-04', 'Labor Day', NULL),
       ('US', '2028-11-23', 'Thanksgiving Day', NULL),
       ('US', '2028-12-25', 'Christmas Day', NULL),

       -- India Market 2026
       ('IN', '2026-01-15', 'Municipal Corporation Election - Maharashtra', NULL),
       ('IN', '2026-01-26', 'Republic Day', NULL),
       ('IN', '2026-03-03', 'Holi', NULL),
       ('IN', '2026-03-26', 'Shri Ram Navami', NULL),
       ('IN', '2026-03-31', 'Shri Mahavir Jayanti', NULL),
       ('IN', '2026-04-03', 'Good Friday', NULL),
       ('IN', '2026-04-14', 'Dr. Baba Saheb Ambedkar Jayanti', NULL),
       ('IN', '2026-05-01', 'Maharashtra Day', NULL),
       ('IN', '2026-05-28', 'Bakri Id', NULL),
       ('IN', '2026-06-26', 'Muharram', NULL),
       ('IN', '2026-09-14', 'Ganesh Chaturthi', NULL),
       ('IN', '2026-10-02', 'Mahatma Gandhi Jayanti', NULL),
       ('IN', '2026-10-20', 'Dussehra', NULL),
       ('IN', '2026-11-10', 'Diwali-Balipratipada', NULL),
       ('IN', '2026-11-24', 'Prakash Gurpurb Sri Guru Nanak Dev', NULL),
       ('IN', '2026-12-25', 'Christmas', NULL)
ON CONFLICT (country, holiday_date) DO NOTHING;

