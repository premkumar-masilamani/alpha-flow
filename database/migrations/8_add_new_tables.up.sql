-- File Statuses
CREATE TABLE file_statuses (
    file_status_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    "date" DATE NOT NULL,
    source VARCHAR(50) NOT NULL,
    download_status VARCHAR(20) DEFAULT 'pending',
    processing_status VARCHAR(20) DEFAULT 'pending',
    processed_time TIMESTAMP,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Daily Data
CREATE TABLE daily_data (
    daily_data_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    "date" DATE NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    open NUMERIC(18, 4),
    high NUMERIC(18, 4),
    low NUMERIC(18, 4),
    close NUMERIC(18, 4),
    volume BIGINT,
    vwap NUMERIC(18, 4),
    buyer_capital_ratio NUMERIC(10, 4),
    buyer_participation_ratio NUMERIC(10, 4),
    buyer_volume_ratio NUMERIC(10, 4),
    whale_impact NUMERIC(10, 4),
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);
