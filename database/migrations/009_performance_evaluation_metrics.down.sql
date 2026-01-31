-- Migration to remove performance evaluation metrics and scoring columns from backtest_results

ALTER TABLE backtest_results
DROP COLUMN IF EXISTS total_return_pct,
DROP COLUMN IF EXISTS max_drawdown_pct,
DROP COLUMN IF EXISTS sharpe_ratio,
DROP COLUMN IF EXISTS total_trades,
DROP COLUMN IF EXISTS avg_win,
DROP COLUMN IF EXISTS avg_loss,
DROP COLUMN IF EXISTS profit_factor,
DROP COLUMN IF EXISTS expectancy,
DROP COLUMN IF EXISTS cagr_score,
DROP COLUMN IF EXISTS mdd_score,
DROP COLUMN IF EXISTS sharpe_score,
DROP COLUMN IF EXISTS profit_factor_score,
DROP COLUMN IF EXISTS expectancy_score,
DROP COLUMN IF EXISTS final_score,
DROP COLUMN IF EXISTS passes_filters;
