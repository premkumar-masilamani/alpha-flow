-- Function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all relevant tables
DO $$
DECLARE
  tbl TEXT;
BEGIN
  FOR tbl IN
    SELECT unnest(ARRAY[
      'zones', 'districts', 'ulbs',
      'schemes', 'components', 'financial_years', 'component_works'
    ])
  LOOP
    EXECUTE format('
      CREATE TRIGGER trg_%s_set_timestamp
      BEFORE UPDATE ON %I
      FOR EACH ROW
      EXECUTE FUNCTION update_timestamp();
    ', tbl, tbl);
  END LOOP;
END;
$$;
