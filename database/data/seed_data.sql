-- Populate Zones
INSERT INTO zones (zone_name) VALUES
('Chennai'),
('Coimbatore'),
('Madurai'),
('Trichy'),
('Salem');

-- Populate Districts
INSERT INTO districts (district_name, zone_id) VALUES
('Chennai', (SELECT zone_id FROM zones WHERE zone_name = 'Chennai')),
('Chengalpattu', (SELECT zone_id FROM zones WHERE zone_name = 'Chennai')),
('Coimbatore', (SELECT zone_id FROM zones WHERE zone_name = 'Coimbatore')),
('Tiruppur', (SELECT zone_id FROM zones WHERE zone_name = 'Coimbatore')),
('Madurai', (SELECT zone_id FROM zones WHERE zone_name = 'Madurai')),
('Dindigul', (SELECT zone_id FROM zones WHERE zone_name = 'Madurai')),
('Trichy', (SELECT zone_id FROM zones WHERE zone_name = 'Trichy')),
('Karur', (SELECT zone_id FROM zones WHERE zone_name = 'Trichy')),
('Salem', (SELECT zone_id FROM zones WHERE zone_name = 'Salem')),
('Namakkal', (SELECT zone_id FROM zones WHERE zone_name = 'Salem'));

-- Populate ULBs
INSERT INTO ulbs (ulb_name, ulb_code, district_id) VALUES
('Greater Chennai Corporation', '800001', (SELECT district_id FROM districts WHERE district_name = 'Chennai')),
('Tambaram', '800002', (SELECT district_id FROM districts WHERE district_name = 'Chengalpattu')),
('Coimbatore Corporation', '803985', (SELECT district_id FROM districts WHERE district_name = 'Coimbatore')),
('Pollachi', '803986', (SELECT district_id FROM districts WHERE district_name = 'Coimbatore')),
('Tiruppur Corporation', '804001', (SELECT district_id FROM districts WHERE district_name = 'Tiruppur')),
('Madurai Corporation', '805001', (SELECT district_id FROM districts WHERE district_name = 'Madurai')),
('Dindigul Municipality', '805002', (SELECT district_id FROM districts WHERE district_name = 'Dindigul')),
('Trichy Corporation', '806001', (SELECT district_id FROM districts WHERE district_name = 'Trichy')),
('Karur Municipality', '806002', (SELECT district_id FROM districts WHERE district_name = 'Karur')),
('Salem Corporation', '807001', (SELECT district_id FROM districts WHERE district_name = 'Salem')),
('Namakkal Municipality', '807002', (SELECT district_id FROM districts WHERE district_name = 'Namakkal'));

-- Populate Financial Years
INSERT INTO financial_years (financial_year_name) VALUES
('2021-2022'),
('2022-2023'),
('2023-2024'),
('2024-2025'),
('2025-2026')
ON CONFLICT (financial_year_name) DO NOTHING;

-- Populate Schemes
INSERT INTO schemes (scheme_name) VALUES
('Capital Grant Fund (CGF)'),
('O & M Gap filling Fund (O&M)'),
('Kalingar Nagarpura Mempattu Thittam (KNMT)'),
('Namakku Nammae Thittam (NNT)'),
('Nagarpura Salaigal Mempattu Thittam (NSMT)'),
('National Bank for Agriculture and Rural Development (NABARD)'),
('Tamil Nadu Urban Road Infrastructure Project (TURIP)'),
('Tamilnadu Urban Wage Employment Scheme (TNUES)'),
('Swachh Bharat Mission 2.0'),
('15th Finance Commission Tied'),
('16th Finance Commission Tied'),
('Smart Cities Mission'),
('Jal Jeevan Mission (Urban)')
ON CONFLICT (scheme_name) DO NOTHING;

-- Populate Components
INSERT INTO components (component_name, scheme_id)
SELECT component_name, scheme_id FROM (VALUES
    ('Road Construction', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Kalingar Nagarpura Mempattu Thittam (KNMT)')),
    ('Drainage Improvement', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Kalingar Nagarpura Mempattu Thittam (KNMT)')),
    ('Water Supply Infrastructure', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Jal Jeevan Mission (Urban)')),
    ('Sewerage Network', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Smart Cities Mission')),
    ('Solid Waste Management', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Swachh Bharat Mission 2.0')),
    ('Public Toilets', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Swachh Bharat Mission 2.0')),
    ('Street Lighting', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Nagarpura Salaigal Mempattu Thittam (NSMT)')),
    ('Park Development', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Namakku Nammae Thittam (NNT)')),
    ('Community Hall Construction', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Namakku Nammae Thittam (NNT)')),
    ('School Infrastructure Upgrade', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Capital Grant Fund (CGF)')),
    ('Bridge Construction', (SELECT scheme_id FROM schemes WHERE scheme_name = 'Tamil Nadu Urban Road Infrastructure Project (TURIP)'))
) AS data(component_name, scheme_id)
ON CONFLICT (scheme_id, component_name) DO NOTHING;

-- Populate Roles
INSERT INTO roles (role_name) VALUES ('viewer'), ('admin'), ('tpadmin');
