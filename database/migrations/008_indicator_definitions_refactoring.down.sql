-- ==========================================
-- 8. UNDO INDICATOR DEFINITIONS REFACTORING
-- ==========================================

-- 1. Clear indicator values to avoid constraint issues when restoring structure
TRUNCATE TABLE public.indicator_values;

-- 2. Drop lookup index and foreign key constraint
DROP INDEX IF EXISTS public.idx_indicator_values_lookup;
ALTER TABLE ONLY public.indicator_values DROP CONSTRAINT IF EXISTS fk_indicator_values_definitions;
ALTER TABLE public.indicator_values DROP CONSTRAINT IF EXISTS uk_indicator_values;

-- 3. Drop indicator_id column
ALTER TABLE public.indicator_values DROP COLUMN IF EXISTS indicator_id;

-- 4. Restore original columns to indicator_values
ALTER TABLE public.indicator_values ADD COLUMN timeframe varchar(16) NOT NULL;
ALTER TABLE public.indicator_values ADD COLUMN indicator_type varchar(32) NOT NULL;
ALTER TABLE public.indicator_values ADD COLUMN source varchar(16) NOT NULL;
ALTER TABLE public.indicator_values ADD COLUMN params varchar(128) NOT NULL;

-- 5. Restore original unique constraint and index
ALTER TABLE public.indicator_values
    ADD CONSTRAINT uk_indicator_values UNIQUE (ticker_id, timeframe, indicator_type, source, params, output_name, price_date);
CREATE INDEX idx_indicator_values_lookup
    ON public.indicator_values USING btree (ticker_id, timeframe, price_date DESC);

-- 6. Drop the new indicator_definitions table
DROP TABLE IF EXISTS public.indicator_definitions CASCADE;
