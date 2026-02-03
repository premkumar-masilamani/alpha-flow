-- Migration to add performance evaluation metrics and scoring columns to backtest_results
-- Using IF NOT EXISTS for robustness as requested in review

ALTER TABLE backtest_results
    ADD COLUMN IF NOT EXISTS years               NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS start_date          DATE,
    ADD COLUMN IF NOT EXISTS end_date            DATE,
    ADD COLUMN IF NOT EXISTS initial_equity      NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS final_equity        NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS total_return_pct    NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS max_drawdown_pct    NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS sharpe_ratio        NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS total_trades        INTEGER,
    ADD COLUMN IF NOT EXISTS avg_win             NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS avg_loss            NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS profit_factor       NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS expectancy          NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS cagr_score          INTEGER,
    ADD COLUMN IF NOT EXISTS mdd_score           INTEGER,
    ADD COLUMN IF NOT EXISTS sharpe_score        INTEGER,
    ADD COLUMN IF NOT EXISTS profit_factor_score INTEGER,
    ADD COLUMN IF NOT EXISTS expectancy_score    INTEGER,
    ADD COLUMN IF NOT EXISTS final_score         NUMERIC(28, 8),
    ADD COLUMN IF NOT EXISTS passes_filters      BOOLEAN;
