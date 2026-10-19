-- ==========================================
-- 35. REVERT BOUNDS COLUMN FROM INDICATOR_DEFINITIONS
-- ==========================================

ALTER TABLE public.indicator_definitions
DROP COLUMN IF EXISTS bounds;
