-- ====================================================================
-- 36. PURGE LEGACY ANGEL_ONE DATA PROVIDER AND NIFTY50 TICKER
-- ====================================================================

-- Update any tickers still configured with ANGEL_ONE to YAHOO_FINANCE
UPDATE public.tickers
SET data_provider = 'YAHOO_FINANCE'
WHERE data_provider = 'ANGEL_ONE';

-- Remove legacy NIFTY50 ticker if ^NSEI already exists, otherwise migrate symbol to ^NSEI
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.tickers WHERE ticker_symbol = 'NIFTY50') THEN
        IF EXISTS (SELECT 1 FROM public.tickers WHERE ticker_symbol = '^NSEI') THEN
            DELETE FROM public.tickers WHERE ticker_symbol = 'NIFTY50';
        ELSE
            UPDATE public.tickers
            SET ticker_symbol = '^NSEI',
                data_provider = 'YAHOO_FINANCE'
            WHERE ticker_symbol = 'NIFTY50';
        END IF;
    END IF;
END $$;
