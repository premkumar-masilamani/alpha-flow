-- ==========================================
-- 37. ADD PRICE_DATE TO CHART PATTERNS
-- ==========================================

ALTER TABLE public.daily_chart_patterns
    ADD COLUMN IF NOT EXISTS price_date DATE NOT NULL DEFAULT CURRENT_DATE;

ALTER TABLE public.daily_chart_patterns
    DROP CONSTRAINT IF EXISTS uk_daily_chart_patterns;

ALTER TABLE public.daily_chart_patterns
    ADD CONSTRAINT uk_daily_chart_patterns UNIQUE (ticker_id, price_date, pattern_type, start_date);

DROP INDEX IF EXISTS public.idx_daily_chart_patterns_status_date;

CREATE INDEX IF NOT EXISTS idx_daily_chart_patterns_ticker_price_status_end
    ON public.daily_chart_patterns (ticker_id, price_date, status, end_date DESC);

ALTER TABLE public.weekly_chart_patterns
    ADD COLUMN IF NOT EXISTS price_date DATE NOT NULL DEFAULT CURRENT_DATE;

ALTER TABLE public.weekly_chart_patterns
    DROP CONSTRAINT IF EXISTS uk_weekly_chart_patterns;

ALTER TABLE public.weekly_chart_patterns
    ADD CONSTRAINT uk_weekly_chart_patterns UNIQUE (ticker_id, price_date, pattern_type, start_date);

DROP INDEX IF EXISTS public.idx_weekly_chart_patterns_status_date;

CREATE INDEX IF NOT EXISTS idx_weekly_chart_patterns_ticker_price_status_end
    ON public.weekly_chart_patterns (ticker_id, price_date, status, end_date DESC);
