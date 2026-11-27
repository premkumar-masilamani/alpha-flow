-- ==========================================
-- 37. ROLLBACK PRICE_DATE ON CHART PATTERNS
-- ==========================================

DROP INDEX IF EXISTS public.idx_daily_chart_patterns_ticker_price_status_end;

ALTER TABLE public.daily_chart_patterns
    DROP CONSTRAINT IF EXISTS uk_daily_chart_patterns;

ALTER TABLE public.daily_chart_patterns
    DROP COLUMN IF EXISTS price_date;

ALTER TABLE public.daily_chart_patterns
    ADD CONSTRAINT uk_daily_chart_patterns UNIQUE (ticker_id, pattern_type, start_date);

CREATE INDEX IF NOT EXISTS idx_daily_chart_patterns_status_date
    ON public.daily_chart_patterns (ticker_id, status, end_date DESC);

DROP INDEX IF EXISTS public.idx_weekly_chart_patterns_ticker_price_status_end;

ALTER TABLE public.weekly_chart_patterns
    DROP CONSTRAINT IF EXISTS uk_weekly_chart_patterns;

ALTER TABLE public.weekly_chart_patterns
    DROP COLUMN IF EXISTS price_date;

ALTER TABLE public.weekly_chart_patterns
    ADD CONSTRAINT uk_weekly_chart_patterns UNIQUE (ticker_id, pattern_type, start_date);

CREATE INDEX IF NOT EXISTS idx_weekly_chart_patterns_status_date
    ON public.weekly_chart_patterns (ticker_id, status, end_date DESC);
