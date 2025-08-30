-- The tables has to be dropped in this specific order
-- because of the referential integrity between the tables
DROP TABLE component_works;
DROP TABLE financial_years;
DROP TABLE components;
DROP TABLE schemes;
DROP TABLE ulbs;
DROP TABLE districts;
DROP TABLE zones;
