ALTER TABLE daily_support_resistances
    ADD COLUMN IF NOT EXISTS first_touch_date DATE,
    ADD COLUMN IF NOT EXISTS last_touch_date DATE;

ALTER TABLE weekly_support_resistances
    ADD COLUMN IF NOT EXISTS first_touch_date DATE,
    ADD COLUMN IF NOT EXISTS last_touch_date DATE;
