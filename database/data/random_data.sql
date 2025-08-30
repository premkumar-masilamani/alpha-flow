-- Populate component_works with random data for all combinations
INSERT INTO component_works (
    component_work_name,
    is_announced,
    administrative_sanction_date,
    technical_sanction_date,
    tender_date,
    work_order_date,
    completion_date,
    estimate_amount,
    expenditure_amount,
    completion_percent,
    road_length,
    current_phase_remarks,
    overall_remarks,
    component_id,
    ulb_id,
    financial_year_id
)
SELECT
    'Work ' || FLOOR(random() * 10000)::INT,
    (random() > 0.5)::BOOLEAN,
    (NOW() - (random() * 365 * 5)::INT * INTERVAL '1 day')::DATE,
    (NOW() - (random() * 365 * 4)::INT * INTERVAL '1 day')::DATE,
    (NOW() - (random() * 365 * 3)::INT * INTERVAL '1 day')::DATE,
    (NOW() - (random() * 365 * 2)::INT * INTERVAL '1 day')::DATE,
    (NOW() - (random() * 365 * 1)::INT * INTERVAL '1 day')::DATE,
    ROUND((random() * 100)::NUMERIC, 2),
    ROUND((random() * 100)::NUMERIC, 2),
    ROUND((random() * 100)::NUMERIC, 2),
    ROUND((random() * 50)::NUMERIC, 2),
    'Current phase remarks for work ' || FLOOR(random() * 1000000)::INT,
    'Overall remarks for work ' || FLOOR(random() * 1000000)::INT,
    c.component_id,
    u.ulb_id,
    fy.financial_year_id
FROM ulbs u
CROSS JOIN components c
CROSS JOIN financial_years fy
ON CONFLICT (ulb_id, component_id, financial_year_id) DO NOTHING;

-- Populate users with 'admin' and 'user'
INSERT INTO users (user_name, password_hash) VALUES
('admin', '$2a$10$3z.B.d/y5.GW5s1a.N.q9u4o2.p4C.h9U8.W5x3e.Y/8.Z9.x8.2a'),
('user', '$2a$10$3z.B.d/y5.GW5s1a.N.q9u4o2.p4C.h9U8.W5x3e.Y/8.Z9.x8.2a')
ON CONFLICT (user_name) DO NOTHING;

-- Populate roles with 'ADMIN' and 'USER'
INSERT INTO roles (role_name) VALUES
('ADMIN'),
('USER')
ON CONFLICT (role_name) DO NOTHING;

-- Assign 'ADMIN' role to 'admin' user
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.user_name = 'admin' AND r.role_name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Assign 'USER' role to 'user' user
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.user_name = 'user' AND r.role_name = 'USER'
ON CONFLICT DO NOTHING;

-- Populate sessions for 'admin' user
INSERT INTO sessions (session_token, user_id, expires_at)
SELECT
    md5(random()::text),
    u.user_id,
    NOW() + INTERVAL '1 day'
FROM users u
WHERE u.user_name = 'admin'
ON CONFLICT (session_token) DO NOTHING;
