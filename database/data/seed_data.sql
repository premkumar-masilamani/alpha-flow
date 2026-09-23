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
       ('^NDX', 'NASDAQ 100', 'INDEX', 'US', true, 'YAHOO_FINANCE'),
       ('^NSEI', 'NIFTY 50', 'INDEX', 'IN', true, 'YAHOO_FINANCE'),
       ('^NSEBANK', 'BANK NIFTY', 'INDEX', 'IN', true, 'YAHOO_FINANCE'),
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
