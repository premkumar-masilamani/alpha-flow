-- ==========================================
-- 2. INDEPENDENT FOREIGN KEY CONSTRAINTS
-- ==========================================

ALTER TABLE ONLY public.daily_prices
    ADD CONSTRAINT fk_daily_prices_tickers FOREIGN KEY (ticker_id)
        REFERENCES public.tickers (ticker_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.weekly_prices
    ADD CONSTRAINT fk_weekly_prices_tickers FOREIGN KEY (ticker_id)
        REFERENCES public.tickers (ticker_id) ON DELETE CASCADE;
