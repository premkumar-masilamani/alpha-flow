-- ==========================================
-- 10. REMOVE TIMEFRAME FROM DEFINITIONS
-- ==========================================

-- 1. Clear existing definitions and values
TRUNCATE TABLE public.indicator_values CASCADE;
TRUNCATE TABLE public.indicator_definitions CASCADE;

-- 2. Drop unique constraint
ALTER TABLE public.indicator_definitions DROP CONSTRAINT IF EXISTS uk_indicator_definitions;

-- 3. Drop the timeframe column
ALTER TABLE public.indicator_definitions DROP COLUMN timeframe;

-- 4. Recreate unique constraint without timeframe
ALTER TABLE public.indicator_definitions
    ADD CONSTRAINT uk_indicator_definitions UNIQUE (indicator_type, source, params);
