CREATE OR REPLACE FUNCTION PUBLIC.NOTIFY() RETURNS trigger AS

$BODY$

BEGIN

    PERFORM pg_notify('pass', row_to_json(NEW)::text);

    RETURN new;

END;

$BODY$

    LANGUAGE 'plpgsql' VOLATILE COST 100;