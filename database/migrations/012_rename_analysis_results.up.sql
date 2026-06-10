ALTER TABLE public.analysis_results RENAME TO asta_results;
ALTER TABLE public.asta_results RENAME CONSTRAINT analysis_results_pkey TO asta_results_pkey;
ALTER TABLE public.asta_results RENAME CONSTRAINT fk_analysis_results_tickers TO fk_asta_results_tickers;

-- Drop the unique constraint on ticker_id
ALTER TABLE public.asta_results DROP CONSTRAINT uk_analysis_results;

-- Add a composite unique constraint on (ticker_id, price_date)
ALTER TABLE public.asta_results ADD CONSTRAINT uk_asta_results UNIQUE (ticker_id, price_date);

-- Rename primary key sequence
ALTER SEQUENCE public.analysis_results_id_seq RENAME TO asta_results_id_seq;
