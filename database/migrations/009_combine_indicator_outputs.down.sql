-- ==========================================
-- 9. UNDO COMBINE INDICATOR OUTPUTS TO JSONB
-- ==========================================

-- 1. Clear indicator values to avoid constraint issues when restoring structure
TRUNCATE TABLE public.indicator_values;

-- 2. Drop the values column
ALTER TABLE public.indicator_values DROP COLUMN IF EXISTS values;

-- 3. Restore output_name and value columns
ALTER TABLE public.indicator_values ADD COLUMN output_name varchar(32) NOT NULL;
ALTER TABLE public.indicator_values ADD COLUMN value numeric(18, 4) NOT NULL;

-- 4. Restore original unique constraint (with output_name)
ALTER TABLE public.indicator_values DROP CONSTRAINT IF EXISTS uk_indicator_values;
ALTER TABLE public.indicator_values
    ADD CONSTRAINT uk_indicator_values UNIQUE (ticker_id, indicator_id, price_date, output_name);

-- 5. Restore lookup index
DROP INDEX IF EXISTS public.idx_indicator_values_lookup;
CREATE INDEX idx_indicator_values_lookup
    ON public.indicator_values USING btree (ticker_id, indicator_id, price_date DESC);
