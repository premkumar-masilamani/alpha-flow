-- ===================================================
-- 26. ADD TARGET AND STOP LOSS PRICES TO CHART PATTERNS
-- ===================================================

ALTER TABLE public.daily_chart_patterns
    ADD COLUMN IF NOT EXISTS target_price NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS stop_loss_price NUMERIC(18, 4);

ALTER TABLE public.weekly_chart_patterns
    ADD COLUMN IF NOT EXISTS target_price NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS stop_loss_price NUMERIC(18, 4);
