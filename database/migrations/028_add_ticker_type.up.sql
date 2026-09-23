-- ==========================================
-- 28. ADD TICKER_TYPE COLUMN TO TICKERS TABLE
-- ==========================================

ALTER TABLE public.tickers
ADD COLUMN IF NOT EXISTS ticker_type VARCHAR(20) NOT NULL DEFAULT 'STOCK';
