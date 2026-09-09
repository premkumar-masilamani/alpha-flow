ALTER TABLE daily_support_resistances
    DROP COLUMN IF EXISTS first_touch_date,
    DROP COLUMN IF EXISTS last_touch_date;

ALTER TABLE weekly_support_resistances
    DROP COLUMN IF EXISTS first_touch_date,
    DROP COLUMN IF EXISTS last_touch_date;
