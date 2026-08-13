-- ==========================================
-- 22. ALTER CANDLESTICK PATTERN SENTIMENT LENGTH
-- ==========================================

ALTER TABLE public.daily_candlestick_patterns ALTER COLUMN sentiment TYPE VARCHAR(32);
ALTER TABLE public.weekly_candlestick_patterns ALTER COLUMN sentiment TYPE VARCHAR(32);
