-- 1. Rename existing 'STOCKS' ticker_type to 'US-EQUITY'
UPDATE tickers
SET ticker_type = 'US-EQUITY'
WHERE ticker_type = 'STOCKS';

-- 2. Ensure LLY, AVGO, BRK-B are present and active as US-EQUITY
INSERT INTO tickers (ticker_date, ticker_symbol, ticker_name, is_active, ticker_type)
VALUES ('1900-01-01', 'BRK-B', 'Berkshire Hathaway', true, 'US-EQUITY'),
       ('1900-01-01', 'LLY', 'Eli Lilly', true, 'US-EQUITY'),
       ('1900-01-01', 'AVGO', 'Broadcom', true, 'US-EQUITY')
ON CONFLICT (ticker_symbol) 
DO UPDATE SET is_active = true, ticker_type = 'US-EQUITY';

UPDATE tickers
SET is_active = true, ticker_type = 'US-EQUITY'
WHERE ticker_symbol IN ('AAPL', 'MSFT', 'GOOGL', 'AMZN', 'META', 'NVDA', 'TSLA');

-- 3. Insert top 10 Nifty-50 tickers under category 'IN-EQUITY'
INSERT INTO tickers (ticker_date, ticker_symbol, ticker_name, is_active, ticker_type)
VALUES ('1900-01-01', 'RELIANCE.NS', 'Reliance Industries', true, 'IN-EQUITY'),
       ('1900-01-01', 'HDFCBANK.NS', 'HDFC Bank', true, 'IN-EQUITY'),
       ('1900-01-01', 'ICICIBANK.NS', 'ICICI Bank', true, 'IN-EQUITY'),
       ('1900-01-01', 'INFY.NS', 'Infosys', true, 'IN-EQUITY'),
       ('1900-01-01', 'TCS.NS', 'Tata Consultancy Services', true, 'IN-EQUITY'),
       ('1900-01-01', 'ITC.NS', 'ITC Limited', true, 'IN-EQUITY'),
       ('1900-01-01', 'LT.NS', 'Larsen & Toubro', true, 'IN-EQUITY'),
       ('1900-01-01', 'AXISBANK.NS', 'Axis Bank', true, 'IN-EQUITY'),
       ('1900-01-01', 'BHARTIARTL.NS', 'Bharti Airtel', true, 'IN-EQUITY'),
       ('1900-01-01', 'KOTAKBANK.NS', 'Kotak Mahindra Bank', true, 'IN-EQUITY')
ON CONFLICT (ticker_symbol)
DO UPDATE SET is_active = true, ticker_type = 'IN-EQUITY';
