INSERT INTO tickers (ticker_symbol, ticker_name, ticker_type, country, is_active)
VALUES ('BRK-B', 'Berkshire Hathaway', 'STOCK', 'US', true),
       ('LLY', 'Eli Lilly', 'STOCK', 'US', true),
       ('AVGO', 'Broadcom', 'STOCK', 'US', true),
       ('AAPL', 'Apple Inc.', 'STOCK', 'US', true),
       ('MSFT', 'Microsoft Corporation', 'STOCK', 'US', true),
       ('GOOGL', 'Alphabet Inc. (Class A)', 'STOCK', 'US', true),
       ('AMZN', 'Amazon.com, Inc.', 'STOCK', 'US', true),
       ('META', 'Meta Platforms, Inc.', 'STOCK', 'US', true),
       ('NVDA', 'NVIDIA Corporation', 'STOCK', 'US', true),
       ('TSLA', 'Tesla, Inc.', 'STOCK', 'US', true),
       ('^GSPC', 'S&P 500', 'INDEX', 'US', true),
       ('^DJI', 'Dow Jones Industrial Average', 'INDEX', 'US', true),
       ('^NDX', 'NASDAQ 100', 'INDEX', 'US', true),
       ('^NSEI', 'NIFTY 50', 'INDEX', 'IN', true),
       ('^NSEBANK', 'BANK NIFTY', 'INDEX', 'IN', true);

INSERT INTO public.indicator_definitions (indicator_type, source, params)
VALUES ('EMA', 'CLOSE', '{"period": 5}'),
       ('EMA', 'CLOSE', '{"period": 13}'),
       ('EMA', 'CLOSE', '{"period": 26}'),
       ('RSI', 'CLOSE', '{"period": 14}'),
       ('STOCHASTIC', 'CLOSE', '{"k": 14, "kSmooth": 3, "dSmooth": 3}'),
       ('SMA', 'VOLUME', '{"period": 20}'),
       ('MACD', 'CLOSE', '{"fast": 12, "slow": 26, "signal": 9}'),
       ('BB', 'CLOSE', '{"period": 20, "stdDev": 2}');
