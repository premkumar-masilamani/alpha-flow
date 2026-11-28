-- ==========================================
-- 39. DROP STATE JSONB COLUMN FROM DAILY AND WEEKLY INDICATORS
-- ==========================================

ALTER TABLE public.weekly_indicators
    DROP COLUMN IF EXISTS state;

ALTER TABLE public.daily_indicators
    DROP COLUMN IF EXISTS state;
