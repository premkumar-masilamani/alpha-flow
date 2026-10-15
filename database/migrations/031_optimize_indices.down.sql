-- ==========================================
-- 31. REVERT DATABASE INDICES OPTIMIZATION
-- ==========================================

-- 5. Drop foreign key indexes on indicator_definition_id
DROP INDEX IF EXISTS public.idx_daily_indicators_definition;
DROP INDEX IF EXISTS public.idx_weekly_indicators_definition;

-- 4. Restore original status index and drop composite status + end_date index
CREATE INDEX IF NOT EXISTS idx_daily_chart_patterns_status ON public.daily_chart_patterns (ticker_id, status);
CREATE INDEX IF NOT EXISTS idx_weekly_chart_patterns_status ON public.weekly_chart_patterns (ticker_id, status);
DROP INDEX IF EXISTS public.idx_daily_chart_patterns_status_date;
DROP INDEX IF EXISTS public.idx_weekly_chart_patterns_status_date;

-- 3. Drop composite index on tickers
DROP INDEX IF EXISTS public.idx_tickers_provider_active;

-- 2. Drop case-insensitive index on ticker_symbol
DROP INDEX IF EXISTS public.idx_tickers_symbol_lower;

-- 1. Restore redundant indexes on support/resistance tables
CREATE INDEX IF NOT EXISTS idx_dsr_ticker_date ON public.daily_support_resistances (ticker_id, price_date);
CREATE INDEX IF NOT EXISTS idx_wsr_ticker_date ON public.weekly_support_resistances (ticker_id, price_date);
