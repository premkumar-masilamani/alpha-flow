-- ==========================================
-- 10. UNDO REMOVE TIMEFRAME FROM DEFINITIONS
-- ==========================================

-- 1. Clear tables
TRUNCATE TABLE public.indicator_values CASCADE;
TRUNCATE TABLE public.indicator_definitions CASCADE;

-- 2. Drop unique constraint
ALTER TABLE public.indicator_definitions DROP CONSTRAINT IF EXISTS uk_indicator_definitions;

-- 3. Add timeframe column back
ALTER TABLE public.indicator_definitions ADD COLUMN timeframe varchar(16) NOT NULL;

-- 4. Recreate unique constraint with timeframe
ALTER TABLE public.indicator_definitions
    ADD CONSTRAINT uk_indicator_definitions UNIQUE (timeframe, indicator_type, source, params);
