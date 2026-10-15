-- ==========================================
-- 31. OPTIMIZE DATABASE INDICES
-- ==========================================

-- 1. Drop redundant indexes covered by unique constraints
DROP INDEX IF EXISTS public.idx_dsr_ticker_date;
DROP INDEX IF EXISTS public.idx_wsr_ticker_date;

-- 2. Add case-insensitive index on ticker_symbol for Ticker lookups
CREATE INDEX IF NOT EXISTS idx_tickers_symbol_lower ON public.tickers (LOWER(ticker_symbol));

-- 3. Add composite index on tickers for downloader gating queries
CREATE INDEX IF NOT EXISTS idx_tickers_provider_active ON public.tickers (data_provider, is_active);

-- 4. Replace single-column status index with composite status + end_date index for chart patterns
CREATE INDEX IF NOT EXISTS idx_daily_chart_patterns_status_date ON public.daily_chart_patterns (ticker_id, status, end_date DESC);
CREATE INDEX IF NOT EXISTS idx_weekly_chart_patterns_status_date ON public.weekly_chart_patterns (ticker_id, status, end_date DESC);
DROP INDEX IF EXISTS public.idx_daily_chart_patterns_status;
DROP INDEX IF EXISTS public.idx_weekly_chart_patterns_status;

-- 5. Add missing foreign key indexes on indicator_definition_id
CREATE INDEX IF NOT EXISTS idx_daily_indicators_definition ON public.daily_indicators (indicator_definition_id);
CREATE INDEX IF NOT EXISTS idx_weekly_indicators_definition ON public.weekly_indicators (indicator_definition_id);
