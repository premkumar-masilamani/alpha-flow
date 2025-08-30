ALTER TABLE component_works DROP CONSTRAINT uq_ulb_id_component_id_financial_year_id;
ALTER TABLE components DROP CONSTRAINT uq_scheme_id_component_name;
ALTER TABLE component_works DROP CONSTRAINT fk_component_works_ulbs;
ALTER TABLE component_works DROP CONSTRAINT fk_component_works_financial_years;
ALTER TABLE component_works DROP CONSTRAINT fk_component_works_components;
ALTER TABLE components DROP CONSTRAINT fk_components_schemes;
ALTER TABLE ulbs DROP CONSTRAINT fk_ulbs_districts;
ALTER TABLE districts DROP CONSTRAINT fk_districts_zones;
