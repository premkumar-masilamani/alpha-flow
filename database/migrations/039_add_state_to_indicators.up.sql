-- ==========================================
-- 39. ADD STATE JSONB COLUMN TO DAILY AND WEEKLY INDICATORS
-- ==========================================

ALTER TABLE public.daily_indicators
    ADD COLUMN IF NOT EXISTS state JSONB;

ALTER TABLE public.weekly_indicators
    ADD COLUMN IF NOT EXISTS state JSONB;
