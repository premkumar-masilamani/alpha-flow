CREATE TABLE tickers (
    ticker_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    start_date TIMESTAMPTZ
);

CREATE TABLE intervals (
    interval_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label VARCHAR(10) NOT NULL UNIQUE,   -- e.g., '1m', '5m', '1h', '1d'
    duration INTERVAL NOT NULL           -- e.g., '1 minute', '5 minutes', '1 hour'
);

CREATE TABLE files (
    file_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticker_id BIGINT NOT NULL,
    file_date TIMESTAMPTZ NOT NULL,
    source VARCHAR(50) NOT NULL,
    is_downloaded BOOLEAN DEFAULT FALSE,
    is_processed BOOLEAN DEFAULT FALSE,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Partitioned table
CREATE TABLE trade_data (
    ticker_id BIGINT NOT NULL,
    interval_id SMALLINT NOT NULL,
    trade_time TIMESTAMPTZ NOT NULL,

    price_open DOUBLE PRECISION,
    price_high DOUBLE PRECISION,
    price_low DOUBLE PRECISION,
    price_close DOUBLE PRECISION,
    volume DOUBLE PRECISION,
    vwap DOUBLE PRECISION,
    buyer_capital_ratio DOUBLE PRECISION,
    buyer_participation_ratio DOUBLE PRECISION,
    buyer_volume_ratio DOUBLE PRECISION,
    whale_impact DOUBLE PRECISION
) PARTITION BY RANGE (trade_time);
