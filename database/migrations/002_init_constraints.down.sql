ALTER TABLE ONLY public.weekly_prices
    DROP CONSTRAINT IF EXISTS fk_weekly_prices_tickers;
ALTER TABLE ONLY public.daily_prices
    DROP CONSTRAINT IF EXISTS fk_daily_prices_tickers;
