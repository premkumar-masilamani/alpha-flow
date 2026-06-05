-- Revert volume column to bigint in daily_prices and weekly_prices
ALTER TABLE public.daily_prices ALTER COLUMN volume TYPE bigint;
ALTER TABLE public.weekly_prices ALTER COLUMN volume TYPE bigint;
