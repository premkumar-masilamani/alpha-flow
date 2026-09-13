-- ========================================================
-- 26. DROP TARGET AND STOP LOSS PRICES FROM CHART PATTERNS
-- ========================================================

ALTER TABLE public.daily_chart_patterns
    DROP COLUMN IF EXISTS target_price,
    DROP COLUMN IF EXISTS stop_loss_price;

ALTER TABLE public.weekly_chart_patterns
    DROP COLUMN IF EXISTS target_price,
    DROP COLUMN IF EXISTS stop_loss_price;
