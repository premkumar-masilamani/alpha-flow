INSERT INTO tickers (ticker_symbol, ticker_name, is_active)
VALUES ('BRK-B', 'Berkshire Hathaway', true),
       ('LLY', 'Eli Lilly', true),
       ('AVGO', 'Broadcom', true),
       ('AAPL', 'Apple Inc.', true),
       ('MSFT', 'Microsoft Corporation', true),
       ('GOOGL', 'Alphabet Inc. (Class A)', true),
       ('AMZN', 'Amazon.com, Inc.', true),
       ('META', 'Meta Platforms, Inc.', true),
       ('NVDA', 'NVIDIA Corporation', true),
       ('TSLA', 'Tesla, Inc.', true);

INSERT INTO public.indicator_definitions (indicator_type, source, params)
VALUES ('EMA', 'CLOSE', '{"period": 5}'),
       ('EMA', 'CLOSE', '{"period": 13}'),
       ('EMA', 'CLOSE', '{"period": 26}'),
       ('RSI', 'CLOSE', '{"period": 14}'),
       ('STOCHASTIC', 'CLOSE', '{"k": 14, "kSmooth": 3, "dSmooth": 3}'),
       ('SMA', 'VOLUME', '{"period": 20}'),
       ('MACD', 'CLOSE', '{"fast": 12, "slow": 26, "signal": 9}'),
       ('VWBB', 'CLOSE', '{"period": 20, "stdDev": 2}');
