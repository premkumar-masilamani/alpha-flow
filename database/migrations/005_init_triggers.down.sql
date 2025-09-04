-- Remove trigger from all relevant tables
DO $$
DECLARE
  tbl TEXT;
BEGIN
  FOR tbl IN
    SELECT unnest(ARRAY['files'])
  LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS trg_%s_set_timestamp ON %I;', tbl, tbl);
  END LOOP;
END;
$$;

-- Remove function to auto-update updated_at
DROP FUNCTION IF EXISTS update_timestamp();
