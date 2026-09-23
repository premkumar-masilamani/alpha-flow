-- ==========================================
-- 30. REVERT INTRADAY_PRICES AND TICKER DATA_PROVIDER
-- ==========================================

DROP TABLE IF EXISTS public.intraday_prices CASCADE;

ALTER TABLE public.tickers
DROP COLUMN IF EXISTS instrument_token;

ALTER TABLE public.tickers
DROP COLUMN IF EXISTS data_provider;
