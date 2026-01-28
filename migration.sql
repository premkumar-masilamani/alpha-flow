-- Migration Script to refactor backtest results persistence

-- 1. Create backtest_equity_daily table
CREATE TABLE backtest_equity_daily (
    id BIGSERIAL PRIMARY KEY,
    ticker_id BIGINT NOT NULL,
    strategy_name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    equity DECIMAL(28, 8) NOT NULL,
    position VARCHAR(50) NOT NULL,
    price_close DECIMAL(28, 8) NOT NULL,
    CONSTRAINT fk_equity_ticker FOREIGN KEY (ticker_id) REFERENCES tickers(ticker_id)
);

-- 2. Create backtest_signal_intent table
CREATE TABLE backtest_signal_intent (
    id BIGSERIAL PRIMARY KEY,
    ticker_id BIGINT NOT NULL,
    strategy_name VARCHAR(255) NOT NULL,
    signal_date DATE NOT NULL,
    execute_date DATE,
    action VARCHAR(50) NOT NULL,
    from_position VARCHAR(50) NOT NULL,
    to_position VARCHAR(50) NOT NULL,
    CONSTRAINT fk_signal_ticker FOREIGN KEY (ticker_id) REFERENCES tickers(ticker_id)
);

-- 3. Create backtest_trade table
CREATE TABLE backtest_trade (
    trade_id UUID PRIMARY KEY,
    ticker_id BIGINT NOT NULL,
    strategy_name VARCHAR(255) NOT NULL,
    side VARCHAR(50) NOT NULL,
    entry_date DATE NOT NULL,
    entry_price DECIMAL(28, 8) NOT NULL,
    exit_date DATE,
    exit_price DECIMAL(28, 8),
    quantity DECIMAL(28, 8) NOT NULL,
    pnl DECIMAL(28, 8),
    holding_bars INTEGER,
    CONSTRAINT fk_trade_ticker FOREIGN KEY (ticker_id) REFERENCES tickers(ticker_id)
);

-- 4. Remove old backtests table
DROP TABLE IF EXISTS backtests;
