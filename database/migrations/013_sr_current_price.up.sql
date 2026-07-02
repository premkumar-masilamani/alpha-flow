ALTER TABLE public.support_resistances
    ADD COLUMN current_price NUMERIC(18, 4) NOT NULL DEFAULT 0;

ALTER TABLE public.support_resistances
    ALTER COLUMN current_price DROP DEFAULT;
