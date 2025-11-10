CREATE TABLE tickers (
    ticker_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL
);

CREATE TABLE files (
    ticker_id INT NOT NULL,
    file_date TIMESTAMPTZ NOT NULL,

    file_download_url VARCHAR(255) NOT NULL,
    is_downloaded BOOLEAN DEFAULT FALSE NOT NULL,
    is_processed BOOLEAN DEFAULT FALSE NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

CREATE TABLE trade_data (
    trade_time TIMESTAMPTZ NOT NULL,
    ticker_id INT NOT NULL,

    price_open NUMERIC(18,8),
    price_high NUMERIC(18,8),
    price_low NUMERIC(18,8),
    price_close NUMERIC(18,8),
    volume NUMERIC(18,8),
    vwap NUMERIC(18,8),
    buyer_volume_ratio REAL,
    buyer_capital_ratio REAL
);
