-- ==========================================
-- 3. PERFORMANCE INDEXES
-- ==========================================

CREATE INDEX idx_daily_prices_lookup
    ON public.daily_prices USING btree (ticker_id, price_date DESC);

CREATE INDEX idx_weekly_prices_lookup
    ON public.weekly_prices USING btree (ticker_id, price_date DESC);