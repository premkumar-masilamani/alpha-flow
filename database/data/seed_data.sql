INSERT INTO tickers (ticker_symbol, ticker_name, ticker_type, country, is_active, data_provider, instrument_token)
VALUES ('BRK-B', 'Berkshire Hathaway', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('LLY', 'Eli Lilly', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('AVGO', 'Broadcom', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('AAPL', 'Apple Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('MSFT', 'Microsoft Corporation', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('GOOGL', 'Alphabet Inc. (Class A)', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('AMZN', 'Amazon.com, Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('META', 'Meta Platforms, Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('NVDA', 'NVIDIA Corporation', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('TSLA', 'Tesla, Inc.', 'STOCK', 'US', true, 'YAHOO_FINANCE', null),
       ('^GSPC', 'S&P 500', 'INDEX', 'US', true, 'YAHOO_FINANCE', null),
       ('^DJI', 'Dow Jones Industrial Average', 'INDEX', 'US', true, 'YAHOO_FINANCE', null),
       ('^NDX', 'NASDAQ 100', 'INDEX', 'US', true, 'YAHOO_FINANCE', null),
       ('^NSEI', 'NIFTY 50', 'INDEX', 'IN', true, 'YAHOO_FINANCE', null),
       ('^NSEBANK', 'BANK NIFTY', 'INDEX', 'IN', true, 'YAHOO_FINANCE', null),
       ('NIFTY50', 'NIFTY 50', 'INDEX', 'IN', true, 'ANGEL_ONE', '99926000');

INSERT INTO public.indicator_definitions (indicator_type, source, params)
VALUES ('EMA', 'CLOSE', '{"period": 5}'),
       ('EMA', 'CLOSE', '{"period": 13}'),
       ('EMA', 'CLOSE', '{"period": 26}'),
       ('RSI', 'CLOSE', '{"period": 14}'),
       ('STOCHASTIC', 'CLOSE', '{"k": 14, "kSmooth": 3, "dSmooth": 3}'),
       ('SMA', 'VOLUME', '{"period": 20}'),
       ('MACD', 'CLOSE', '{"fast": 12, "slow": 26, "signal": 9}'),
       ('BB', 'CLOSE', '{"period": 20, "stdDev": 2}');
