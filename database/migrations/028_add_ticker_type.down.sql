-- ==========================================
-- 28. REVERT TICKER_TYPE COLUMN FROM TICKERS TABLE
-- ==========================================

ALTER TABLE public.tickers
DROP COLUMN IF EXISTS ticker_type;
