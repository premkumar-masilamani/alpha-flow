CREATE SEQUENCE IF NOT EXISTS asta_results_id_seq START 1 INCREMENT 1;

CREATE TABLE asta_results (
  analysis_id   bigint NOT NULL DEFAULT nextval('asta_results_id_seq') PRIMARY KEY,
  ticker_id     bigint NOT NULL REFERENCES tickers(ticker_id) ON DELETE CASCADE,
  price_date    date   NOT NULL,
  ema_signal    varchar(32)  NOT NULL,
  ema_value     varchar(255) NOT NULL,
  macd_signal   varchar(32)  NOT NULL,
  macd_value    varchar(255) NOT NULL,
  stochastic_signal varchar(32)  NOT NULL,
  stochastic_value  varchar(255) NOT NULL,
  rsi_signal    varchar(32)  NOT NULL,
  rsi_value     varchar(255) NOT NULL,
  volume_signal varchar(32)  NOT NULL,
  volume_value  varchar(255) NOT NULL,
  overall_signal varchar(32) NOT NULL,
  CONSTRAINT asta_results_ticker_price_date_unique UNIQUE (ticker_id, price_date)
);
