CREATE TABLE IF NOT EXISTS daily_support_resistances (
    id SERIAL PRIMARY KEY,
    ticker_id BIGINT NOT NULL REFERENCES tickers(ticker_id) ON DELETE CASCADE,
    price_date DATE NOT NULL,
    zone_bottom NUMERIC NOT NULL,
    zone_top NUMERIC NOT NULL,
    zone_midpoint NUMERIC NOT NULL,
    touch_count INTEGER NOT NULL,
    level_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ticker_id, price_date, zone_bottom, zone_top)
);

CREATE INDEX IF NOT EXISTS idx_dsr_ticker_date ON daily_support_resistances(ticker_id, price_date);

CREATE TABLE IF NOT EXISTS weekly_support_resistances (
    id SERIAL PRIMARY KEY,
    ticker_id BIGINT NOT NULL REFERENCES tickers(ticker_id) ON DELETE CASCADE,
    price_date DATE NOT NULL,
    zone_bottom NUMERIC NOT NULL,
    zone_top NUMERIC NOT NULL,
    zone_midpoint NUMERIC NOT NULL,
    touch_count INTEGER NOT NULL,
    level_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ticker_id, price_date, zone_bottom, zone_top)
);

CREATE INDEX IF NOT EXISTS idx_wsr_ticker_date ON weekly_support_resistances(ticker_id, price_date);
