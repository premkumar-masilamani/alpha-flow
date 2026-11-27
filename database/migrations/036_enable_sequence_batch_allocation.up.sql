-- ====================================================================
-- 36. ENABLE SEQUENCE BATCH ALLOCATION (INCREMENT BY 50)
-- ====================================================================

-- 1. Standardize legacy identity sequence names from pre-rename tables
ALTER SEQUENCE IF EXISTS public.daily_prices_id_seq
    RENAME TO daily_prices_daily_price_id_seq;

ALTER SEQUENCE IF EXISTS public.weekly_prices_id_seq
    RENAME TO weekly_prices_weekly_price_id_seq;

ALTER SEQUENCE IF EXISTS public.daily_candlestick_patterns_id_seq
    RENAME TO daily_candlestick_patterns_daily_candlestick_pattern_id_seq;

ALTER SEQUENCE IF EXISTS public.weekly_candlestick_patterns_id_seq
    RENAME TO weekly_candlestick_patterns_weekly_candlestick_pattern_id_seq;

-- 2. Set identity sequence increment to 50 for high-volume engine tables
ALTER TABLE public.daily_prices
    ALTER COLUMN daily_price_id SET INCREMENT BY 50;

ALTER TABLE public.weekly_prices
    ALTER COLUMN weekly_price_id SET INCREMENT BY 50;

ALTER TABLE public.daily_indicators
    ALTER COLUMN daily_indicator_id SET INCREMENT BY 50;

ALTER TABLE public.weekly_indicators
    ALTER COLUMN weekly_indicator_id SET INCREMENT BY 50;

ALTER TABLE public.daily_support_resistances
    ALTER COLUMN daily_support_resistance_id SET INCREMENT BY 50;

ALTER TABLE public.weekly_support_resistances
    ALTER COLUMN weekly_support_resistance_id SET INCREMENT BY 50;

ALTER TABLE public.daily_candlestick_patterns
    ALTER COLUMN daily_candlestick_pattern_id SET INCREMENT BY 50;

ALTER TABLE public.weekly_candlestick_patterns
    ALTER COLUMN weekly_candlestick_pattern_id SET INCREMENT BY 50;

ALTER TABLE public.daily_chart_patterns
    ALTER COLUMN daily_chart_pattern_id SET INCREMENT BY 50;

ALTER TABLE public.weekly_chart_patterns
    ALTER COLUMN weekly_chart_pattern_id SET INCREMENT BY 50;
