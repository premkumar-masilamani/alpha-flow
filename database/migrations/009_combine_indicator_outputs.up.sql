-- ==========================================
-- 9. COMBINE INDICATOR OUTPUTS TO JSONB
-- ==========================================

-- 1. Clear existing indicator values
TRUNCATE TABLE public.indicator_values;

-- 2. Drop the output_name and value columns
ALTER TABLE public.indicator_values DROP COLUMN output_name;
ALTER TABLE public.indicator_values DROP COLUMN value;

-- 3. Add the values JSONB column
ALTER TABLE public.indicator_values ADD COLUMN values jsonb NOT NULL;

-- 4. Recreate the unique constraint (without output_name)
ALTER TABLE public.indicator_values DROP CONSTRAINT IF EXISTS uk_indicator_values;
ALTER TABLE public.indicator_values
    ADD CONSTRAINT uk_indicator_values UNIQUE (ticker_id, indicator_id, price_date);

-- 5. Recreate the lookup index (which is already ticker_id, indicator_id, price_date DESC)
DROP INDEX IF EXISTS public.idx_indicator_values_lookup;
CREATE INDEX idx_indicator_values_lookup
    ON public.indicator_values USING btree (ticker_id, indicator_id, price_date DESC);
