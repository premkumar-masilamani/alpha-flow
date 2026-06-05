-- Alter volume column to numeric(18, 4) in daily_prices and weekly_prices
ALTER TABLE public.daily_prices ALTER COLUMN volume TYPE numeric(18, 4);
ALTER TABLE public.weekly_prices ALTER COLUMN volume TYPE numeric(18, 4);
