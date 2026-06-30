CREATE TYPE sr_filter_reason AS ENUM (
    'BREAKS_GT_2',
    'TOUCHES_LT_3',
    'TOUCHES_LT_4',
    'PROXIMITY_1_PCT',
    'CIRCUIT_BREAKER_20_PCT'
);

ALTER TABLE daily_sr ADD COLUMN filter_reason sr_filter_reason;
ALTER TABLE weekly_sr ADD COLUMN filter_reason sr_filter_reason;
