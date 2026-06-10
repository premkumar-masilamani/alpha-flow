ALTER TABLE public.asta_results RENAME TO analysis_results;
ALTER TABLE public.analysis_results RENAME CONSTRAINT asta_results_pkey TO analysis_results_pkey;
ALTER TABLE public.analysis_results RENAME CONSTRAINT fk_asta_results_tickers TO fk_analysis_results_tickers;

-- Drop composite unique constraint
ALTER TABLE public.analysis_results DROP CONSTRAINT uk_asta_results;

-- Re-add unique constraint on ticker_id
ALTER TABLE public.analysis_results ADD CONSTRAINT uk_analysis_results UNIQUE (ticker_id);

-- Rename primary key sequence back
ALTER SEQUENCE public.asta_results_id_seq RENAME TO analysis_results_id_seq;
