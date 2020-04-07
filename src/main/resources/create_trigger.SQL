CREATE TRIGGER PASS_EXPORT

    AFTER INSERT OR UPDATE

    ON tudelft_oopp.public."user"

    FOR EACH ROW

EXECUTE PROCEDURE public.notify();