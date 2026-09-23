-- ==========================================
-- 29. ADD COUNTRY COLUMN TO TICKERS TABLE
-- ==========================================

ALTER TABLE public.tickers
ADD COLUMN IF NOT EXISTS country VARCHAR(10) NOT NULL DEFAULT 'US';
