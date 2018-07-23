CREATE OR REPLACE FUNCTION get_dhis_test_id(dhis_test_name VARCHAR(255))
  RETURNS NUMERIC
AS'
  DECLARE
    dhis_test_id NUMERIC;
BEGIN
  SELECT id
  INTO dhis_test_id
  FROM dhis2_test
  WHERE lower(test_name) like lower(dhis_test_name);
  RETURN dhis_test_id;
END'
LANGUAGE plpgsql;
