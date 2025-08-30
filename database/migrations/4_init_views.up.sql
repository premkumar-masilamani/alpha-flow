CREATE OR REPLACE VIEW vw_component_work_summary AS
SELECT
    row_number() OVER () AS id,

    cw.component_work_name,
    cw.is_announced,

    cw.administrative_sanction_date,
    cw.technical_sanction_date,
    cw.tender_date,
    cw.work_order_date,
    cw.completion_date,

    cw.estimate_amount,
    cw.expenditure_amount,
    cw.completion_percent,
    cw.road_length,

    cw.current_phase_remarks,
    cw.overall_remarks,

    fy.financial_year_name,
    c.component_name,
    s.scheme_name,
    u.ulb_name,
    u.ulb_code,
    d.district_name,
    z.zone_name,

    cw.updated_at AS last_updated,
    cw.updated_by AS last_updated_by

FROM component_works cw
JOIN financial_years fy ON cw.financial_year_id = fy.financial_year_id
JOIN components c ON cw.component_id = c.component_id
JOIN schemes s ON c.scheme_id = s.scheme_id
JOIN ulbs u ON cw.ulb_id = u.ulb_id
JOIN districts d ON u.district_id = d.district_id
JOIN zones z ON d.zone_id = z.zone_id;
