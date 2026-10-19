-- ==========================================
-- 35. ADD BOUNDS COLUMN TO INDICATOR_DEFINITIONS
-- ==========================================

ALTER TABLE public.indicator_definitions
ADD COLUMN IF NOT EXISTS bounds jsonb;
