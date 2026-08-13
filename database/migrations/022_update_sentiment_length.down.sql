-- ==========================================
-- 22. ALTER CANDLESTICK PATTERN SENTIMENT LENGTH (DOWN)
-- ==========================================

ALTER TABLE public.daily_candlestick_patterns ALTER COLUMN sentiment TYPE VARCHAR(10);
ALTER TABLE public.weekly_candlestick_patterns ALTER COLUMN sentiment TYPE VARCHAR(10);
