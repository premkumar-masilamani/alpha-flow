-- ==========================================
-- 35. REMOVE INTRADAY AND LIVE QUOTE TABLES
-- ==========================================

DROP TABLE IF EXISTS public.live_quotes CASCADE;
DROP TABLE IF EXISTS public.intraday_prices CASCADE;
