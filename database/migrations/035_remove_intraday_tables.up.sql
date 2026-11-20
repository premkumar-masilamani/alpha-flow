-- ====================================================================
-- 35. PURGE INTRADAY, LIVE QUOTES, NOTIFICATIONS, HOLIDAYS & DATA_PROVIDER
-- ====================================================================

-- 1. Drop intraday and live tables
DROP TABLE IF EXISTS public.live_quotes CASCADE;
DROP TABLE IF EXISTS public.intraday_prices CASCADE;

-- 2. Drop notification logs and market holidays
DROP TABLE IF EXISTS public.notification_logs CASCADE;
DROP TABLE IF EXISTS public.market_holidays CASCADE;

-- 3. Drop data_provider column and its index from tickers
DROP INDEX IF EXISTS public.idx_tickers_provider_active CASCADE;
ALTER TABLE public.tickers DROP COLUMN IF EXISTS data_provider CASCADE;
