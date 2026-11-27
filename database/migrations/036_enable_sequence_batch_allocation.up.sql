-- ==========================================
-- 36. SWITCH ENGINE PRIMARY KEYS FROM IDENTITY TO SEQUENCES (INCREMENT BY 50)
-- ==========================================

ALTER TABLE public.daily_prices ALTER COLUMN daily_price_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.daily_prices_seq START WITH 1 INCREMENT BY 50 OWNED BY public.daily_prices.daily_price_id;
ALTER TABLE public.daily_prices ALTER COLUMN daily_price_id SET DEFAULT nextval('public.daily_prices_seq');

ALTER TABLE public.weekly_prices ALTER COLUMN weekly_price_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.weekly_prices_seq START WITH 1 INCREMENT BY 50 OWNED BY public.weekly_prices.weekly_price_id;
ALTER TABLE public.weekly_prices ALTER COLUMN weekly_price_id SET DEFAULT nextval('public.weekly_prices_seq');

ALTER TABLE public.daily_indicators ALTER COLUMN daily_indicator_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.daily_indicators_seq START WITH 1 INCREMENT BY 50 OWNED BY public.daily_indicators.daily_indicator_id;
ALTER TABLE public.daily_indicators ALTER COLUMN daily_indicator_id SET DEFAULT nextval('public.daily_indicators_seq');

ALTER TABLE public.weekly_indicators ALTER COLUMN weekly_indicator_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.weekly_indicators_seq START WITH 1 INCREMENT BY 50 OWNED BY public.weekly_indicators.weekly_indicator_id;
ALTER TABLE public.weekly_indicators ALTER COLUMN weekly_indicator_id SET DEFAULT nextval('public.weekly_indicators_seq');

ALTER TABLE public.daily_support_resistances ALTER COLUMN daily_support_resistance_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.daily_support_resistances_seq START WITH 1 INCREMENT BY 50 OWNED BY public.daily_support_resistances.daily_support_resistance_id;
ALTER TABLE public.daily_support_resistances ALTER COLUMN daily_support_resistance_id SET DEFAULT nextval('public.daily_support_resistances_seq');

ALTER TABLE public.weekly_support_resistances ALTER COLUMN weekly_support_resistance_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.weekly_support_resistances_seq START WITH 1 INCREMENT BY 50 OWNED BY public.weekly_support_resistances.weekly_support_resistance_id;
ALTER TABLE public.weekly_support_resistances ALTER COLUMN weekly_support_resistance_id SET DEFAULT nextval('public.weekly_support_resistances_seq');

ALTER TABLE public.daily_candlestick_patterns ALTER COLUMN daily_candlestick_pattern_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.daily_candlestick_patterns_seq START WITH 1 INCREMENT BY 50 OWNED BY public.daily_candlestick_patterns.daily_candlestick_pattern_id;
ALTER TABLE public.daily_candlestick_patterns ALTER COLUMN daily_candlestick_pattern_id SET DEFAULT nextval('public.daily_candlestick_patterns_seq');

ALTER TABLE public.weekly_candlestick_patterns ALTER COLUMN weekly_candlestick_pattern_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.weekly_candlestick_patterns_seq START WITH 1 INCREMENT BY 50 OWNED BY public.weekly_candlestick_patterns.weekly_candlestick_pattern_id;
ALTER TABLE public.weekly_candlestick_patterns ALTER COLUMN weekly_candlestick_pattern_id SET DEFAULT nextval('public.weekly_candlestick_patterns_seq');

ALTER TABLE public.daily_chart_patterns ALTER COLUMN daily_chart_pattern_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.daily_chart_patterns_seq START WITH 1 INCREMENT BY 50 OWNED BY public.daily_chart_patterns.daily_chart_pattern_id;
ALTER TABLE public.daily_chart_patterns ALTER COLUMN daily_chart_pattern_id SET DEFAULT nextval('public.daily_chart_patterns_seq');

ALTER TABLE public.weekly_chart_patterns ALTER COLUMN weekly_chart_pattern_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS public.weekly_chart_patterns_seq START WITH 1 INCREMENT BY 50 OWNED BY public.weekly_chart_patterns.weekly_chart_pattern_id;
ALTER TABLE public.weekly_chart_patterns ALTER COLUMN weekly_chart_pattern_id SET DEFAULT nextval('public.weekly_chart_patterns_seq');
