-- Foreign Key Constraints
ALTER TABLE districts ADD CONSTRAINT fk_districts_zones
  FOREIGN KEY (zone_id) REFERENCES zones(zone_id) ON DELETE RESTRICT;

ALTER TABLE ulbs ADD CONSTRAINT fk_ulbs_districts
  FOREIGN KEY (district_id) REFERENCES districts(district_id) ON DELETE RESTRICT;

ALTER TABLE components ADD CONSTRAINT fk_components_schemes
  FOREIGN KEY (scheme_id) REFERENCES schemes(scheme_id) ON DELETE RESTRICT;

ALTER TABLE component_works ADD CONSTRAINT fk_component_works_ulbs
  FOREIGN KEY (ulb_id) REFERENCES ulbs(ulb_id) ON DELETE RESTRICT;

ALTER TABLE component_works ADD CONSTRAINT fk_component_works_components
  FOREIGN KEY (component_id) REFERENCES components(component_id) ON DELETE RESTRICT;

ALTER TABLE component_works ADD CONSTRAINT fk_component_works_financial_years
  FOREIGN KEY (financial_year_id) REFERENCES financial_years(financial_year_id) ON DELETE RESTRICT;

-- Unique Constraints
ALTER TABLE components ADD CONSTRAINT uq_scheme_id_component_name UNIQUE (scheme_id, component_name);
ALTER TABLE component_works ADD CONSTRAINT uq_ulb_id_component_id_financial_year_id UNIQUE (ulb_id, component_id, financial_year_id);
