CREATE TABLE tickers
(
    ticker_id  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    start_date DATE         NOT NULL,
    symbol     VARCHAR(20)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL
);

CREATE TABLE files
(
    file_id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticker_id     INT                   NOT NULL,
    file_date     DATE                  NOT NULL,
    file_url      TEXT                  NOT NULL,
    is_downloaded BOOLEAN DEFAULT FALSE NOT NULL,
    is_processed  BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE trade_data
(
    trade_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticker_id           INT  NOT NULL,
    trade_date          DATE NOT NULL,
    price_open          NUMERIC(18, 8),
    price_high          NUMERIC(18, 8),
    price_low           NUMERIC(18, 8),
    price_close         NUMERIC(18, 8),
    volume              NUMERIC(28, 8),
    vwap                NUMERIC(18, 8),
    buyer_volume_ratio  DOUBLE PRECISION,
    buyer_capital_ratio DOUBLE PRECISION
);
