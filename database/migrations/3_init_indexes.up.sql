-- Indexes for performance
CREATE INDEX idx_districts_zone_id ON districts(zone_id);
CREATE INDEX idx_ulbs_district_id ON ulbs(district_id);
CREATE INDEX idx_components_scheme_id ON components(scheme_id);
CREATE INDEX idx_component_works_ulb_id ON component_works(ulb_id);
CREATE INDEX idx_component_works_component_id ON component_works(component_id);
CREATE INDEX idx_component_works_financial_year_id ON component_works(financial_year_id);
