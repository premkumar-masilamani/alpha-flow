DROP INDEX IF EXISTS public.idx_indicator_values_lookup;

ALTER TABLE ONLY public.indicator_state
    DROP CONSTRAINT IF EXISTS fk_indicator_state_tickers;
ALTER TABLE ONLY public.indicator_values
    DROP CONSTRAINT IF EXISTS fk_indicator_values_tickers;

DROP TABLE IF EXISTS public.indicator_state;
DROP TABLE IF EXISTS public.indicator_values;
