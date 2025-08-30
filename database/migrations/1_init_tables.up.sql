-- Zones
CREATE TABLE zones (
    zone_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    zone_name VARCHAR(100) NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Districts
CREATE TABLE districts (
    district_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    district_name VARCHAR(100) NOT NULL,
    zone_id INT NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Urban Local Bodies
CREATE TABLE ulbs (
    ulb_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ulb_name VARCHAR(150) NOT NULL,
    ulb_code VARCHAR(6) NOT NULL,
    district_id INT NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Schemes
CREATE TABLE schemes (
    scheme_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scheme_name VARCHAR(150) UNIQUE NOT NULL,
    scheme_abbr VARCHAR(15),
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Components
CREATE TABLE components (
    component_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_name VARCHAR(150) NOT NULL,
    scheme_id INT NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Financial Years
CREATE TABLE financial_years (
    financial_year_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    financial_year_name VARCHAR(10) UNIQUE NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Component Works
CREATE TABLE component_works (
    component_work_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_work_name VARCHAR(200) NOT NULL,
    is_announced BOOLEAN DEFAULT FALSE,
    administrative_sanction_date DATE,
    technical_sanction_date DATE,
    tender_date DATE,
    work_order_date DATE,
    completion_date DATE,
    estimate_amount NUMERIC(12, 2),
    expenditure_amount NUMERIC(12, 2),
    completion_percent NUMERIC,
    road_length NUMERIC,
    current_phase_remarks TEXT,
    overall_remarks TEXT,
    component_id INT NOT NULL,
    ulb_id INT NOT NULL,
    financial_year_id INT NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);
