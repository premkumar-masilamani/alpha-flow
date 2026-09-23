-- ==========================================
-- 29. REVERT COUNTRY COLUMN FROM TICKERS TABLE
-- ==========================================

ALTER TABLE public.tickers
DROP COLUMN IF EXISTS country;
